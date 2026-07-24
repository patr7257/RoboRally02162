// Host-authoritative activation loop. Polls submitted player intents,
// activates a round once everyone (or the sole occupant) is ready, and
// publishes readiness diffs to the shared envelope. Only the host tab runs
// this loop; player tabs read the results back over the transport.

import { MoveType } from "../../types/boardTypes";
import {
  submitProgram,
  allSubmitted,
  runActivation,
} from "../../engine/roborally-engine";
import { moveTypesToCards } from "../engineAdapter";
import { BASE, jsonFetch, hostHeaders, getIdentity, putState } from "./transport";
import type { Envelope } from "./transport";
import { getEnv, setEnv, getAnimating, setAnimating } from "./store";

const HOST_LOOP_MS = 1500;

let hostTimer: ReturnType<typeof setInterval> | null = null;
let hostProgram: MoveType[] | null = null;
let submittedThisRound = false;
let lastReadiness: Record<number, boolean> = {};
let onConflict: ((fresh: Envelope) => void) | null = null;

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

export async function hostTick(force = false): Promise<void> {
  const id = getIdentity();
  if (!id || id.role !== "host" || getAnimating()) return;
  const env = getEnv();
  if (!env?.snap || env.status !== "active" || env.snap.status !== "programming")
    return;

  const round = env.snap.round;
  const r = await jsonFetch(
    `${BASE}/games/${id.gameId}/intents?round=${round}`,
    { headers: hostHeaders() },
  );
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
    const next = { ...env, readiness };
    const ok = await putState(next, onConflict ?? undefined);
    if (ok) setEnv(next);
  }
}

async function activate(
  occupied: number[],
  programByIdx: Record<number, MoveType[]>,
): Promise<void> {
  const env = getEnv();
  if (!env?.snap || getAnimating()) return;
  setAnimating(true); // block re-entry until the PUT lands
  try {
    let snap = env.snap;
    for (const idx of occupied) {
      const prog = programByIdx[idx];
      if (!prog) continue;
      const robotId = idx + 1;
      snap = submitProgram(snap, robotId, moveTypesToCards(prog));
    }
    if (!allSubmitted(snap)) {
      setAnimating(false);
      return;
    }
    const result = runActivation(snap);
    const next: Envelope = {
      ...env,
      status: result.snapshot.winner != null ? "finished" : "active",
      phase: result.snapshot.status,
      round: result.snapshot.round,
      current: 0,
      snap: result.snapshot,
      frames: result.frames,
      activationId: (env.activationId ?? 0) + 1,
      readiness: {},
    };
    hostProgram = null;
    submittedThisRound = false;
    lastReadiness = {};
    setAnimating(false); // let onRev drive the animation from the fresh PUT
    const ok = await putState(next, onConflict ?? undefined);
    if (ok) setEnv(next);
  } catch (e) {
    setAnimating(false);
    console.error("[rrr] activation failed", e);
  }
}

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
}
