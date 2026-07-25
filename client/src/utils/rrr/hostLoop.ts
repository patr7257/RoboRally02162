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
//
// While the game is active the loop also sends a liveness heartbeat every 5s so
// player tabs can tell a dead host from a quiet one (issue #4), and it enforces
// the optional programming deadline when the game was created with a timer
// (issue #9). The acting host is whatever seat this tab holds, not seat 0: after
// a takeover the host seat is the seat of whoever won the election.

import { MoveType } from "../../types/boardTypes";
import {
  submitProgram,
  allSubmitted,
  runActivation,
  resumeActivation,
  applyRespawns,
  parseBoardDefinition,
  createGame,
} from "../../engine/roborally-engine";
import type {
  ActivationResult,
  CardSnapshot,
  Direction,
  GameSnapshot,
  ReactionChoice,
} from "../../engine/roborally-engine";
import { moveTypesToCards } from "../engineAdapter";
import { buildPlayerConfigs } from "../gameSetup";
import {
  BASE,
  hostFetch,
  getIdentity,
  putState,
  sendHeartbeat,
  serverNow,
  wireRound,
} from "./transport";
import type { Envelope, HostPrivate, Identity, Rejection } from "./transport";
import { getEnv, setEnv, getAnimating, setAnimating } from "./store";

const HOST_LOOP_MS = 1500;
/** How long the host waits for a reaction / respawn answer before defaulting. */
const PROMPT_MS = 10000;
/** How often the acting host proves it is still alive. Player tabs treat a beat
 *  older than 12s as a dead host, so this leaves room for two missed beats. */
const BEAT_MS = 5000;

let hostTimer: ReturnType<typeof setInterval> | null = null;
let hostProgram: MoveType[] | null = null;
let submittedThisRound = false;
let lastReadiness: Record<number, boolean> = {};
let onConflict: ((fresh: Envelope) => void) | null = null;
/** The host's answer to its own reaction prompt (no intent round-trip). */
let hostReactionChoice: { promptId: string; choice: ReactionChoice } | null = null;
/** The host's answer to its own respawn prompt. */
let hostRespawnDirection: Direction | null = null;
/** Local time of the last heartbeat sent by this tab. */
let lastBeatSentAt = 0;
/** Set once this tab won a host election: from then on a seat that exists in the
 *  envelope roster but holds no seat claim (the departed original host, who
 *  never posted a seat) is auto-programmed with [] rather than stalling the
 *  round forever (issue #4). */
let autoProgramGhosts = false;

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

export function setAutoProgramGhosts(v: boolean): void {
  autoProgramGhosts = v;
}

/** Sends a heartbeat now and restarts the 5s beat window. Called by the takeover
 *  path so a freshly promoted host is visibly alive immediately. */
export function beatNow(): void {
  lastBeatSentAt = Date.now();
  sendHeartbeat();
}

// ---- helpers ----------------------------------------------------------------

/** The hostPrivate payload to ride along with an outgoing PUT: present only
 *  while the host has a program for the round being published. */
function hostPrivateFor(snap: GameSnapshot): HostPrivate | undefined {
  if (!hostProgram) return undefined;
  return { round: snap.round, hostProgram, submittedThisRound };
}

/** `round` here is already a wire round (see transport.ts's wireRound); every
 *  caller is responsible for converting the engine snapshot's own round before
 *  calling in (issue #10). */
async function fetchIntents(gameId: string, round: number): Promise<any[]> {
  const r = await hostFetch(`${BASE}/games/${gameId}/intents?round=${round}`);
  if (!r.ok || !r.data) return [];
  return (r.data.intents as any[]) || [];
}

function deadlinePassed(env: Envelope): boolean {
  return env.deadlineAt != null && serverNow() > env.deadlineAt;
}

/** The programming timer has run out for this envelope. Only ever true when the
 *  game was created with a timer, so a timer-less game keeps waiting for every
 *  seat exactly as it always did (issue #9). */
function programmingTimerExpired(env: Envelope): boolean {
  return !!env.timerMs && env.deadlineAt != null && serverNow() >= env.deadlineAt;
}

/** The deadline to publish alongside a snapshot: a paused snapshot gets the 10s
 *  prompt window (which takes precedence), a fresh programming phase gets the
 *  optional programming timer, and anything else clears the deadline. */
function deadlineFor(env: Envelope, snap: GameSnapshot): number | null {
  if (snap.status === "awaiting-reaction" || snap.status === "awaiting-respawn") {
    return serverNow() + PROMPT_MS;
  }
  if (snap.status === "programming" && snap.winner == null && env.timerMs) {
    return serverNow() + env.timerMs;
  }
  return null;
}

/** Sends the liveness beat at most once per BEAT_MS. Fire-and-forget: it never
 *  bumps the revision and never awaits, so it cannot slow a tick down. */
function maybeBeat(): void {
  const now = Date.now();
  if (now - lastBeatSentAt < BEAT_MS) return;
  lastBeatSentAt = now;
  sendHeartbeat();
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
  const next: Envelope = {
    ...env,
    status: snap.winner != null ? "finished" : "active",
    phase: snap.status,
    round: wireRound(env, snap.round),
    current: 0,
    snap,
    frames: result.frames,
    activationId: (env.activationId ?? 0) + 1,
    readiness: {},
    deadlineAt: deadlineFor(env, snap),
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
  if (!id || id.role !== "host") return;
  const env = getEnv();
  // Prove liveness before any other guard: an activation animation can outlast
  // the staleness window, and a host mid-animation is very much alive.
  if (env?.status === "active") maybeBeat();
  if (getAnimating()) return;
  if (!env?.snap || env.status !== "active") return;

  switch (env.snap.status) {
    case "programming":
      await tickProgramming(env, id, force);
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
  id: Identity,
  force: boolean,
): Promise<void> {
  const snap = env.snap!;
  const round = snap.round;

  // Timer fallback: round 1 is published by the lobby, and a takeover clears the
  // deadline, so stamp it here when the envelope has a timer but no deadline yet.
  if (env.timerMs && env.deadlineAt == null) {
    const stamped: Envelope = {
      ...env,
      deadlineAt: serverNow() + env.timerMs,
      hostPrivate: hostPrivateFor(snap),
    };
    const ok = await putState(stamped, onConflict ?? undefined);
    if (ok) setEnv(stamped);
    return;
  }

  const r = await hostFetch(`${BASE}/games/${id.gameId}/intents?round=${wireRound(env, round)}`);
  if (!r.ok || !r.data) return;

  const seats: Record<string, unknown> = r.data.seats || {};
  const intents: any[] = r.data.intents || [];

  // The acting host holds whatever seat this tab claimed, which is seat 0 only
  // for the game's original creator.
  const hostSeatIdx = id.seatIdx;
  const occupied = Array.from(
    new Set([hostSeatIdx, ...Object.keys(seats).map(Number)]),
  ).sort((a, b) => a - b);
  // Roster entries with neither a seat claim nor the host role: robots whose tab
  // is gone for good and can never submit anything again.
  const ghosts = (env.players || [])
    .map((p) => p.idx)
    .filter((idx) => !occupied.includes(idx));

  const programByIdx: Record<number, MoveType[]> = {};
  intents
    .filter((it) => it.type === "program" && Array.isArray(it.registers))
    .forEach((it) => {
      programByIdx[it.playerIdx] = it.registers as MoveType[];
    });
  // The acting host's own pick lives in this tab, not in an intent, and wins for
  // its seat: a tab promoted mid-round may have both.
  if (hostProgram) programByIdx[hostSeatIdx] = hostProgram;

  // Publish readiness to all tabs when it changes. Ghosts are left out: they can
  // never become ready, so counting them would freeze the "n/m ready" display.
  const readiness: Record<number, boolean> = {};
  occupied.forEach((idx) => {
    readiness[idx] = programByIdx[idx] != null;
  });
  lastReadiness = readiness;
  const changed = JSON.stringify(env.readiness || {}) !== JSON.stringify(readiness);

  // An expired programming timer auto-programs every seat that has not picked;
  // an empty pick is legal and the engine completes it to 5 from the deck.
  const timedOut = programmingTimerExpired(env);
  if (timedOut) {
    occupied.forEach((idx) => {
      if (programByIdx[idx] == null) programByIdx[idx] = [];
    });
  }
  const includeGhosts = timedOut || autoProgramGhosts;
  if (includeGhosts) {
    ghosts.forEach((idx) => {
      if (programByIdx[idx] == null) programByIdx[idx] = [];
    });
  }

  const allReady = occupied.every((idx) => programByIdx[idx] != null);

  if (allReady && (force || occupied.length >= 1)) {
    await activate(includeGhosts ? [...occupied, ...ghosts] : occupied, programByIdx);
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
      // An auto-programmed [] is a real (empty) pick, not a missing one.
      if (prog == null) continue;
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
    const intents = await fetchIntents(gameId, wireRound(env, snap.round));
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
    const intents = await fetchIntents(gameId, wireRound(env, snap.round));
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

// ---- rematch (issue #10) ------------------------------------------------------

/**
 * Builds a fresh game on the same board and roster and PUTs it over the
 * finished match. Host-only, and only once the match has actually finished:
 * the backend never deletes per-round intent keys and allows a PUT to move
 * status from "finished" back to "active", so reusing engine round numbers
 * would replay the last match's stale program/reaction/respawn intents.
 * roundBase is set to the last wire round the finished match published, so
 * every round the new match plays gets a wire round past anything the old
 * match ever used, and activationId is left UNCHANGED (frames: []) so no tab
 * replays an activation.
 */
export async function requestRematch(): Promise<void> {
  const id = getIdentity();
  if (!id || id.role !== "host") return;
  const env = getEnv();
  if (!env || env.status !== "finished") return;

  const boardUrl = env.board
    ? `${process.env.PUBLIC_URL}/boards/${env.board}.json`
    : `${process.env.PUBLIC_URL}/board.json`;
  let boardRes = await fetch(boardUrl);
  if (!boardRes.ok) boardRes = await fetch(`${process.env.PUBLIC_URL}/board.json`);
  if (!boardRes.ok) return;
  const def = await boardRes.json();
  const loaded = parseBoardDefinition(def);
  const configs = buildPlayerConfigs(loaded, env.players);
  const snap = createGame(loaded.board, configs);

  const next: Envelope = {
    ...env,
    status: "active",
    phase: "programming",
    snap,
    frames: [],
    readiness: {},
    activationId: env.activationId ?? 0,
    matchId: (env.matchId ?? 1) + 1,
    roundBase: env.round,
    round: env.round + snap.round,
    deadlineAt: env.timerMs ? serverNow() + env.timerMs : undefined,
    hostPrivate: undefined,
    rejections: undefined,
  };

  hostProgram = null;
  submittedThisRound = false;
  lastReadiness = {};
  clearPromptStashes();

  const ok = await putState(next, onConflict ?? undefined);
  if (ok) setEnv(next);
}

// ---- lifecycle ---------------------------------------------------------------

/** Starts the host tick timer. `onConflictCb` is invoked with the fresh
 *  envelope whenever a host write loses the optimistic-concurrency race,
 *  mirroring the old inline reconcile() call. No-op for non-host identities. */
export function startHostLoop(onConflictCb: (fresh: Envelope) => void): void {
  const id = getIdentity();
  if (!id || id.role !== "host") return;
  stopHostLoop(); // idempotent: a takeover may restart the loop in place
  onConflict = onConflictCb;
  lastBeatSentAt = 0; // beat on the very first tick
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
  lastBeatSentAt = 0;
  autoProgramGhosts = false;
  clearPromptStashes();
}
