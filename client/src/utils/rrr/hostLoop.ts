// Host-authoritative activation loop. The host tab is the only tab that runs
// this; player tabs read the results back over the transport. Every tick is a
// dispatcher on the authoritative snapshot status:
//   "programming"       - poll program intents, publish readiness, activate
//                         once everyone is ready (issues #3, #14)
//   "awaiting-reaction" - poll the paused robot's reaction choice, resume the
//                         activation when it arrives or the deadline passes (#6)
//   "awaiting-respawn"  - poll respawn directions for the dead robots, reboot
//                         them once all answered or the deadline passes (#5)
//   "finished"          - nothing left to drive
//
// The host answers its own prompts through module stashes instead of posting an
// intent to itself (hostReactionChoice / hostRespawnDirection).

import { MoveType } from "../../types/boardTypes";
import {
  submitProgram,
  allSubmitted,
  runActivation,
  resumeActivation,
  applyRespawns,
} from "../../engine/roborally-engine";
import type {
  ActivationResult,
  CardSnapshot,
  Direction,
  GameSnapshot,
  ReactionChoice,
} from "../../engine/roborally-engine";
import { moveTypesToCards } from "../engineAdapter";
import { BASE, jsonFetch, hostHeaders, getIdentity, putState } from "./transport";
import type { Envelope, HostPrivate, Rejection } from "./transport";
import { getEnv, setEnv, getAnimating, setAnimating } from "./store";

const HOST_LOOP_MS = 1500;
/** How long the host waits for a reaction / respawn answer before defaulting. */
const PROMPT_MS = 10000;

let hostTimer: ReturnType<typeof setInterval> | null = null;
let hostProgram: MoveType[] | null = null;
let submittedThisRound = false;
let lastReadiness: Record<number, boolean> = {};
let onConflict: ((fresh: Envelope) => void) | null = null;
/** The host's answer to its own reaction prompt (no intent round-trip). */
let hostReactionChoice: { promptId: string; choice: ReactionChoice } | null = null;
/** The host's answer to its own respawn prompt. */
let hostRespawnDirection: Direction | null = null;

export function getHostProgram(): MoveType[] | null {
  return hostProgram;
}
export function setHostProgram(v: MoveType[] | null): void {
  hostProgram = v;
}

export function getSubmittedThisRound(): boolean {
  return submittedThisRound;
}
export function setSubmittedThisRound(v: boolean): void {
  submittedThisRound = v;
}

export function getLastReadiness(): Record<number, boolean> {
  return lastReadiness;
}
/** Used by the host-resume path to adopt the readiness published before the
 *  tab was reloaded (issue #3). */
export function setLastReadiness(v: Record<number, boolean>): void {
  lastReadiness = v;
}

export function setHostReactionChoice(
  promptId: string | null | undefined,
  choice: ReactionChoice,
): void {
  hostReactionChoice = promptId ? { promptId, choice } : null;
}

export function setHostRespawnDirection(v: Direction | null): void {
  hostRespawnDirection = v;
}

export function clearPromptStashes(): void {
  hostReactionChoice = null;
  hostRespawnDirection = null;
}

// ---- helpers ----------------------------------------------------------------

/** The hostPrivate payload to ride along with an outgoing PUT: present only
 *  while the host has a program for the round being published. */
function hostPrivateFor(snap: GameSnapshot): HostPrivate | undefined {
  if (!hostProgram) return undefined;
  return { round: snap.round, hostProgram, submittedThisRound };
}

async function fetchIntents(gameId: string, round: number): Promise<any[]> {
  const r = await jsonFetch(`${BASE}/games/${gameId}/intents?round=${round}`, {
    headers: hostHeaders(),
  });
  if (!r.ok || !r.data) return [];
  return (r.data.intents as any[]) || [];
}

function deadlinePassed(env: Envelope): boolean {
  return env.deadlineAt != null && Date.now() > env.deadlineAt;
}

/**
 * Publishes one activation segment (a full round, or the part of a round up to
 * the next pause). A segment that pauses again carries a fresh prompt deadline;
 * anything else clears it. hostPrivate is dropped here: the host's program has
 * been consumed by the engine, so there is nothing left to resume.
 */
async function publishSegment(
  result: ActivationResult,
  rejections?: Record<number, Rejection>,
): Promise<void> {
  const env = getEnv();
  if (!env) return;
  const snap = result.snapshot;
  const awaiting =
    snap.status === "awaiting-reaction" || snap.status === "awaiting-respawn";
  const next: Envelope = {
    ...env,
    status: snap.winner != null ? "finished" : "active",
    phase: snap.status,
    round: snap.round,
    current: 0,
    snap,
    frames: result.frames,
    activationId: (env.activationId ?? 0) + 1,
    readiness: {},
    deadlineAt: awaiting ? Date.now() + PROMPT_MS : null,
    hostPrivate: undefined,
    rejections:
      rejections && Object.keys(rejections).length > 0 ? rejections : undefined,
  };
  hostProgram = null;
  submittedThisRound = false;
  lastReadiness = {};
  hostReactionChoice = null;
  if (snap.round !== env.round) hostRespawnDirection = null;
  setAnimating(false); // let onRev drive the animation from the fresh PUT
  const ok = await putState(next, onConflict ?? undefined);
  if (ok) setEnv(next);
}

// ---- dispatcher --------------------------------------------------------------

export async function hostTick(force = false): Promise<void> {
  const id = getIdentity();
  if (!id || id.role !== "host" || getAnimating()) return;
  const env = getEnv();
  if (!env?.snap || env.status !== "active") return;

  switch (env.snap.status) {
    case "programming":
      await tickProgramming(env, id.gameId, force);
      return;
    case "awaiting-reaction":
      await tickReaction(env, id.gameId, id.robotId);
      return;
    case "awaiting-respawn":
      await tickRespawn(env, id.gameId, id.robotId);
      return;
    default:
      return; // "activating" is transient, "finished"/"lobby" need no driving
  }
}

async function tickProgramming(
  env: Envelope,
  gameId: string,
  force: boolean,
): Promise<void> {
  const snap = env.snap!;
  const round = snap.round;
  const r = await jsonFetch(`${BASE}/games/${gameId}/intents?round=${round}`, {
    headers: hostHeaders(),
  });
  if (!r.ok || !r.data) return;

  const seats: Record<string, unknown> = r.data.seats || {};
  const intents: any[] = r.data.intents || [];

  const occupied = [0, ...Object.keys(seats).map(Number)];
  const programByIdx: Record<number, MoveType[]> = {};
  if (hostProgram) programByIdx[0] = hostProgram;
  intents
    .filter((it) => it.type === "program" && Array.isArray(it.registers))
    .forEach((it) => {
      programByIdx[it.playerIdx] = it.registers as MoveType[];
    });

  // Publish readiness to all tabs when it changes.
  const readiness: Record<number, boolean> = {};
  occupied.forEach((idx) => {
    readiness[idx] = !!programByIdx[idx];
  });
  lastReadiness = readiness;
  const changed = JSON.stringify(env.readiness || {}) !== JSON.stringify(readiness);

  const allReady = occupied.every((idx) => !!programByIdx[idx]);

  if (allReady && (force || occupied.length >= 1)) {
    await activate(occupied, programByIdx);
    return;
  }
  if (changed) {
    const next: Envelope = { ...env, readiness, hostPrivate: hostPrivateFor(snap) };
    const ok = await putState(next, onConflict ?? undefined);
    if (ok) setEnv(next);
  }
}

/**
 * Locks in every submitted program and runs the round. A program the engine
 * rejects (a cheat, or a hand that has moved on since the pick) is replaced by
 * the first five cards of that robot's dealt hand, and the reason is published
 * so the owning tab can tell its player (issue #14). The host's own program
 * goes through the exact same validation.
 */
async function activate(
  occupied: number[],
  programByIdx: Record<number, MoveType[]>,
): Promise<void> {
  const env = getEnv();
  if (!env?.snap || getAnimating()) return;
  setAnimating(true); // block re-entry until the PUT lands
  try {
    let snap = env.snap;
    const rejections: Record<number, Rejection> = {};
    for (const idx of occupied) {
      const prog = programByIdx[idx];
      if (!prog) continue;
      const robotId = idx + 1;
      try {
        snap = submitProgram(snap, robotId, moveTypesToCards(prog));
      } catch (err) {
        const reason = err instanceof Error ? err.message : String(err);
        const hand: CardSnapshot[] = snap.decks[String(robotId)]?.hand ?? [];
        snap = submitProgram(snap, robotId, hand.slice(0, 5));
        // Recorded against the round of the snapshot this PUT publishes, so the
        // rejection surfaces exactly once, on the envelope that carries it.
        rejections[idx] = { round: -1, reason };
      }
    }
    if (!allSubmitted(snap)) {
      setAnimating(false);
      return;
    }
    const result = runActivation(snap);
    Object.keys(rejections).forEach((k) => {
      rejections[Number(k)].round = result.snapshot.round;
    });
    await publishSegment(result, rejections);
  } catch (e) {
    setAnimating(false);
    console.error("[rrr] activation failed", e);
  }
}

/** Resumes a round paused on an interactive card once the owning seat has
 *  chosen, or once the prompt deadline has passed (the engine then applies the
 *  reaction's default choice). */
async function tickReaction(
  env: Envelope,
  gameId: string,
  myRobotId: number,
): Promise<void> {
  const snap = env.snap!;
  const pending = snap.pendingReaction;
  if (!pending) return;

  let choice: ReactionChoice | null = null;
  if (pending.robotId === myRobotId) {
    if (hostReactionChoice?.promptId === pending.promptId) {
      choice = hostReactionChoice.choice;
    }
  } else {
    const intents = await fetchIntents(gameId, snap.round);
    const hit = intents.find(
      (it) =>
        it.type === "reaction" &&
        it.promptId === pending.promptId &&
        Number(it.playerIdx) + 1 === pending.robotId,
    );
    if (hit) choice = (hit.choice as ReactionChoice) ?? null;
  }

  if (choice == null && !deadlinePassed(env)) return;
  if (getAnimating()) return;
  setAnimating(true);
  try {
    await publishSegment(resumeActivation(snap, choice));
  } catch (e) {
    setAnimating(false);
    console.error("[rrr] reaction resume failed", e);
  }
}

/** Reboots the round's dead robots once every one of them has a facing, or once
 *  the prompt deadline has passed (missing entries default engine-side to the
 *  facing the robot died with). */
async function tickRespawn(
  env: Envelope,
  gameId: string,
  myRobotId: number,
): Promise<void> {
  const snap = env.snap!;
  const dead = snap.robots.filter((r) => !r.alive);
  const directions: Record<number, Direction> = {};

  if (dead.some((r) => r.id === myRobotId) && hostRespawnDirection) {
    directions[myRobotId] = hostRespawnDirection;
  }
  if (dead.some((r) => r.id !== myRobotId)) {
    const intents = await fetchIntents(gameId, snap.round);
    intents
      .filter((it) => it.type === "respawn" && it.direction)
      .forEach((it) => {
        const robotId = Number(it.playerIdx) + 1;
        if (dead.some((r) => r.id === robotId)) {
          directions[robotId] = it.direction as Direction;
        }
      });
  }

  const allAnswered = dead.every((r) => !!directions[r.id]);
  if (!allAnswered && !deadlinePassed(env)) return;
  if (getAnimating()) return;
  setAnimating(true);
  try {
    await publishSegment(applyRespawns(snap, directions));
  } catch (e) {
    setAnimating(false);
    console.error("[rrr] respawn failed", e);
  }
}

// ---- lifecycle ---------------------------------------------------------------

/** Starts the host tick timer. `onConflictCb` is invoked with the fresh
 *  envelope whenever a host write loses the optimistic-concurrency race,
 *  mirroring the old inline reconcile() call. No-op for non-host identities. */
export function startHostLoop(onConflictCb: (fresh: Envelope) => void): void {
  const id = getIdentity();
  if (!id || id.role !== "host") return;
  onConflict = onConflictCb;
  hostTimer = setInterval(() => void hostTick(), HOST_LOOP_MS);
}

export function stopHostLoop(): void {
  if (hostTimer) clearInterval(hostTimer);
  hostTimer = null;
}

export function reset(): void {
  stopHostLoop();
  hostProgram = null;
  submittedThisRound = false;
  lastReadiness = {};
  clearPromptStashes();
}
