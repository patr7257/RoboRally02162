// Host-authoritative game controller with the SAME public surface the old
// WebSocket transport exposed (subscribe / sendMessage / closeSocket /
// getSocket / getQueueSize / WebSocketManager). Instead of a Java gateway over
// WebSockets it talks to the same-origin Vercel backend (/api/robot-rally/*):
// inbound over SSE (EventSource on /events), outbound over fetch. The creator's
// tab runs the ported rules engine and is authoritative; it reads player
// program intents, activates a round, and PUTs the next state. Every tab
// animates the returned frames. To keep the existing Board.tsx untouched, this
// module synthesizes the exact JSON-string messages Board already parses
// (stateSnapshot / hand / discard / damageDecks / programmingStarted /
// roundExecuting / gameFinished / ack).
//
// Identity is read from sessionStorage (written by the create/join lobby):
//   rrr_gameId, rrr_role ("host"|"player"), rrr_hostToken, rrr_playerToken,
//   rrr_pw, rrr_seatIdx, rrr_name.

import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import {
  submitProgram,
  allSubmitted,
  runActivation,
} from "../engine/roborally-engine";
import type { GameSnapshot, Frame } from "../engine/roborally-engine";
import {
  snapshotToGameData,
  frameToGameData,
  handForRobot,
  discardForRobot,
  moveTypesToCards,
} from "./engineAdapter";
import { MoveType } from "../types/boardTypes";

export const BASE = "/api/robot-rally";
const FRAME_MS = 320; // slightly above BoardRenderer's 0.3s CSS transition
const HOST_LOOP_MS = 1500;
const FALLBACK_POLL_MS = 3000;
const FAKE_MS_REMAINING = 999000; // no server programming timer; disables auto-submit

/** The state blob stored in Redis. Envelope fields are validated by the backend;
 *  everything under `snap` / `frames` is opaque host data. */
export interface Envelope {
  name: string;
  status: "lobby" | "active" | "paused" | "finished";
  phase: string;
  round: number;
  current: number;
  players: { idx: number; name: string; color?: string }[];
  board?: string;
  snap?: GameSnapshot;
  frames?: Frame[];
  activationId?: number;
  readiness?: Record<number, boolean>;
  // server-managed: gameId, version, createdAt, updatedAt
}

interface Identity {
  gameId: string;
  role: "host" | "player";
  hostToken: string | null;
  playerToken: string | null;
  pw: string;
  seatIdx: number;
  robotId: number;
  name: string;
}

// ---- module state --------------------------------------------------------

let listeners: Set<(message: string) => void> = new Set();
let id: Identity | null = null;
let env: Envelope | null = null;
let version = 0;
let started = false;
let busy = false;
let animating = false;

let es: EventSource | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;
let hostTimer: ReturnType<typeof setInterval> | null = null;

let lastActivationId = -1;
let lastRoundEntered = 0;
let finishedEmitted = false;

// host-only
let hostProgram: MoveType[] | null = null;
let submittedThisRound = false;
let lastReadiness: Record<number, boolean> = {};

// ---- emit + fetch helpers ------------------------------------------------

function emit(obj: unknown): void {
  const s = JSON.stringify(obj);
  listeners.forEach((cb) => cb(s));
}

function readIdentity(): Identity | null {
  const gameId = sessionStorage.getItem("rrr_gameId");
  if (!gameId) return null;
  const role = (sessionStorage.getItem("rrr_role") as "host" | "player") || "player";
  const seatIdx = Number(sessionStorage.getItem("rrr_seatIdx") || "0");
  return {
    gameId,
    role,
    hostToken: sessionStorage.getItem("rrr_hostToken"),
    playerToken: sessionStorage.getItem("rrr_playerToken"),
    pw: sessionStorage.getItem("rrr_pw") || "",
    seatIdx,
    robotId: seatIdx + 1,
    name: sessionStorage.getItem("rrr_name") || `Player ${seatIdx + 1}`,
  };
}

function hostHeaders(): Record<string, string> {
  return {
    "Content-Type": "application/json",
    "x-rrr-host-token": id?.hostToken || "",
  };
}

async function jsonFetch(url: string, opts?: RequestInit) {
  try {
    const res = await fetch(url, opts);
    let data: any = null;
    try {
      data = await res.json();
    } catch {
      data = null;
    }
    return { ok: res.ok, status: res.status, data };
  } catch (e) {
    return { ok: false, status: 0, data: null };
  }
}

/** Fetch the latest authoritative blob. Returns the new envelope, or null if
 *  unchanged / unavailable. */
async function fetchState(): Promise<Envelope | null> {
  if (!id) return null;
  if (id.role === "host") {
    const r = await jsonFetch(`${BASE}/games/${id.gameId}/state`, {
      headers: hostHeaders(),
    });
    if (!r.ok || !r.data) return null;
    version = r.data.version;
    return r.data.state as Envelope;
  }
  const r = await jsonFetch(
    `${BASE}/games/${id.gameId}/view?pw=${encodeURIComponent(id.pw)}&v=${version}`,
  );
  if (!r.ok || !r.data) return null;
  if (r.data.unchanged) return null;
  version = r.data.version;
  return r.data.state as Envelope;
}

/** Host-only optimistic-concurrency write. Returns true on success. */
async function putState(next: Envelope): Promise<boolean> {
  if (!id) return false;
  const r = await jsonFetch(`${BASE}/games/${id.gameId}/state`, {
    method: "PUT",
    headers: hostHeaders(),
    body: JSON.stringify({ baseVersion: version, state: next }),
  });
  if (r.ok && r.data) {
    version = r.data.version;
    env = next;
    return true;
  }
  if (r.status === 409) {
    // Lost the race; resync on the next tick.
    const fresh = await fetchState();
    if (fresh) reconcile(fresh);
  }
  return false;
}

// ---- inbound reconcile + activation animation ----------------------------

function emitState(snap: GameSnapshot): void {
  emit({ type: "stateSnapshot", payload: snapshotToGameData(snap) });
}

function emitHandBundle(snap: GameSnapshot): void {
  if (!id) return;
  emit({ type: "hand", payload: { hand: handForRobot(snap, id.robotId) } });
  emit({ type: "discard", payload: { discard: discardForRobot(snap, id.robotId) } });
  emit({
    type: "damageDecks",
    payload: {
      spamCount: snap.damageDecks.spam,
      trojanHorseCount: snap.damageDecks.trojan,
      wormCount: snap.damageDecks.worm,
    },
  });
}

function enterRoundIfNeeded(snap: GameSnapshot): void {
  if (snap.status !== "programming") return;
  if (snap.round === lastRoundEntered) return;
  lastRoundEntered = snap.round;
  submittedThisRound = false;
  hostProgram = null;
  emit({ type: "programmingStarted" });
  emitHandBundle(snap);
}

function emitReadiness(): void {
  const playerSubmitted: Record<number, boolean> = {};
  if (id?.role === "host") {
    Object.entries(lastReadiness).forEach(([idx, v]) => {
      playerSubmitted[Number(idx) + 1] = v; // key by robotId
    });
  } else if (env?.readiness) {
    Object.entries(env.readiness).forEach(([idx, v]) => {
      playerSubmitted[Number(idx) + 1] = v;
    });
  } else if (id) {
    playerSubmitted[id.robotId] = submittedThisRound;
  }
  emit({
    type: "readiness",
    payload: { playerSubmitted, msRemaining: FAKE_MS_REMAINING },
  });
}

function animateActivation(next: Envelope): Promise<void> {
  return new Promise((resolve) => {
    const snap = next.snap!;
    const frames = next.frames || [];
    animating = true;
    emit({ type: "roundExecuting" });

    let i = 0;
    const step = () => {
      if (i < frames.length) {
        emit({ type: "stateSnapshot", payload: frameToGameData(snap, frames[i]) });
        i++;
        setTimeout(step, FRAME_MS);
      } else {
        emitState(snap); // settle on authoritative positions
        animating = false;
        resolve();
      }
    };
    step();
  });
}

async function reconcile(next: Envelope): Promise<void> {
  const isActivation =
    next.activationId != null &&
    next.activationId !== lastActivationId &&
    (next.frames?.length ?? 0) > 0;

  if (isActivation) {
    lastActivationId = next.activationId!;
    env = next;
    await animateActivation(next);
  } else {
    env = next;
    if (next.snap) emitState(next.snap);
  }

  // Common tail: winner banner or next programming round.
  if (next.snap) {
    if (next.status === "finished" || next.snap.winner != null) {
      if (!finishedEmitted) {
        finishedEmitted = true;
        emit({ type: "gameFinished", payload: { winner: next.snap.winner } });
      }
    } else {
      enterRoundIfNeeded(next.snap);
    }
  }
}

async function onRev(): Promise<void> {
  if (busy || animating) return;
  busy = true;
  try {
    const next = await fetchState();
    if (next) await reconcile(next);
  } finally {
    busy = false;
  }
}

// ---- host activation loop ------------------------------------------------

async function hostTick(force = false): Promise<void> {
  if (!id || id.role !== "host" || animating) return;
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
    await putState({ ...env, readiness });
  }
}

async function activate(
  occupied: number[],
  programByIdx: Record<number, MoveType[]>,
): Promise<void> {
  if (!env?.snap || animating) return;
  animating = true; // block re-entry until the PUT lands
  try {
    let snap = env.snap;
    for (const idx of occupied) {
      const prog = programByIdx[idx];
      if (!prog) continue;
      const robotId = idx + 1;
      snap = submitProgram(snap, robotId, moveTypesToCards(prog));
    }
    if (!allSubmitted(snap)) {
      animating = false;
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
    animating = false; // let onRev drive the animation from the fresh PUT
    await putState(next);
  } catch (e) {
    animating = false;
    console.error("[rrr] activation failed", e);
  }
}

// ---- lifecycle -----------------------------------------------------------

function ensureStarted(): void {
  if (started) return;
  id = readIdentity();
  if (!id) return;
  started = true;

  es = new EventSource(`${BASE}/games/${id.gameId}/events`);
  es.onmessage = () => {
    void onRev();
  };
  es.onerror = () => {
    /* EventSource auto-reconnects; the fallback poll covers gaps */
  };
  pollTimer = setInterval(() => void onRev(), FALLBACK_POLL_MS);
  if (id.role === "host") {
    hostTimer = setInterval(() => void hostTick(), HOST_LOOP_MS);
  }
  void onRev();
}

// ---- public API (unchanged surface) --------------------------------------

/** Legacy WebSocket accessor. No socket exists now; always null. */
export function getSocket(_reason: string): WebSocket | null {
  return null;
}

export function subscribe(cb: (message: string) => void): () => void {
  listeners.add(cb);
  ensureStarted();
  return () => {
    listeners.delete(cb);
  };
}

export function sendMessage(data: string | object): boolean {
  let obj: any;
  try {
    obj = typeof data === "string" ? JSON.parse(data) : data;
  } catch {
    return false;
  }
  const type = obj?.payload?.type;
  const snap = env?.snap;

  switch (type) {
    case "getBoard":
      if (snap) emitState(snap);
      return true;
    case "getHand":
      if (snap && id)
        emit({ type: "hand", payload: { hand: handForRobot(snap, id.robotId) } });
      return true;
    case "getDiscard":
      if (snap && id)
        emit({
          type: "discard",
          payload: { discard: discardForRobot(snap, id.robotId) },
        });
      return true;
    case "getDamageDecks":
      if (snap)
        emit({
          type: "damageDecks",
          payload: {
            spamCount: snap.damageDecks.spam,
            trojanHorseCount: snap.damageDecks.trojan,
            wormCount: snap.damageDecks.worm,
          },
        });
      return true;
    case "getReadiness":
      emitReadiness();
      return true;
    case "getLastMoves":
      return true; // per-move log not synthesized in this slice
    case "startProgramming":
      emit({ type: "programmingStarted" });
      return true;
    case "submitProgram": {
      const cards: MoveType[] = obj.payload.cards || [];
      submittedThisRound = true;
      if (id?.role === "host") {
        hostProgram = cards;
        emit({ type: "ack", payload: { message: "Program submitted" } });
        void hostTick();
      } else {
        void submitProgramIntent(cards);
      }
      return true;
    }
    case "forceStartRound":
      if (id?.role === "host") void hostTick(true);
      return true;
    case "setRespawnDirection":
    case "submitReaction":
      // Deferred to a later slice (engine has the logic; UI collection pending).
      return true;
    default:
      return true;
  }
}

async function submitProgramIntent(cards: MoveType[]): Promise<void> {
  if (!id || !env?.snap) return;
  const r = await jsonFetch(`${BASE}/games/${id.gameId}/intent`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      playerIdx: id.seatIdx,
      playerToken: id.playerToken,
      round: env.snap.round,
      type: "program",
      registers: cards,
    }),
  });
  if (r.ok) {
    emit({ type: "ack", payload: { message: "Program submitted" } });
  } else if (r.status === 409) {
    // Already programmed this round; treat as submitted.
    emit({ type: "ack", payload: { message: "Program submitted" } });
  } else {
    emit({ type: "error", payload: { message: "Failed to submit program" } });
  }
}

export function closeSocket(_reason: number): void {
  if (es) es.close();
  es = null;
  if (pollTimer) clearInterval(pollTimer);
  if (hostTimer) clearInterval(hostTimer);
  pollTimer = null;
  hostTimer = null;
  listeners.clear();
  started = false;
  env = null;
  version = 0;
  lastActivationId = -1;
  lastRoundEntered = 0;
  finishedEmitted = false;
  hostProgram = null;
  submittedThisRound = false;
  lastReadiness = {};
}

export function getQueueSize(): number {
  return 0;
}

/** Current roster (name + robotId) derived from the authoritative envelope.
 *  Used by the board scene in place of the old /api/lobby/getRobot call. */
export function getRoster(): { name: string; robotId: number }[] {
  const players = env?.players || [];
  return players.map((p) => ({ name: p.name, robotId: p.idx + 1 }));
}

/** This tab's own robotId (seat index + 1), or "" before identity is known. */
export function getMyRobotId(): string {
  const ident = id ?? readIdentity();
  return ident ? String(ident.robotId) : "";
}

/** Kept so routes that mounted it to keep the connection alive still compile. */
export const WebSocketManager = () => {
  useLocation();
  useEffect(() => {
    ensureStarted();
  }, []);
  return null;
};
