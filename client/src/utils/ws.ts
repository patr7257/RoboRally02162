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
// This file is the facade: it owns the reconcile/onRev seam that turns
// inbound transport revisions into emitted messages, ensureStarted, and the
// unchanged public exports. The actual state lives in ./rrr/*:
//   rrr/transport.ts - identity, version, SSE + fallback poll
//   rrr/store.ts      - shared session state (env, busy, animating, ...)
//   rrr/emit.ts       - listeners + message synthesis + activation playback
//   rrr/hostLoop.ts   - host-only activation loop
//
// Identity is read from sessionStorage (written by the create/join lobby):
//   rrr_gameId, rrr_role ("host"|"player"), rrr_hostToken, rrr_playerToken,
//   rrr_pw, rrr_seatIdx, rrr_name.

import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { handForRobot, discardForRobot } from "./engineAdapter";
import { MoveType } from "../types/boardTypes";
import {
  BASE,
  getIdentity,
  readIdentity,
  fetchState,
  postIntent,
  startTransport,
  stopTransport,
} from "./rrr/transport";
import type { Envelope } from "./rrr/transport";
import {
  getEnv,
  setEnv,
  getBusy,
  setBusy,
  getAnimating,
  getLastActivationId,
  setLastActivationId,
  getFinishedEmitted,
  setFinishedEmitted,
  reset as resetStore,
} from "./rrr/store";
import {
  addListener,
  emit,
  emitState,
  emitReadiness,
  animateActivation,
  enterRoundIfNeeded,
  reset as resetEmit,
} from "./rrr/emit";
import {
  hostTick,
  startHostLoop,
  setSubmittedThisRound,
  setHostProgram,
  reset as resetHostLoop,
} from "./rrr/hostLoop";

export { BASE };
export type { Envelope };

// ---- module state (facade-local only) --------------------------------------

let started = false;

// ---- inbound reconcile ------------------------------------------------------

async function reconcile(next: Envelope): Promise<void> {
  const isActivation =
    next.activationId != null &&
    next.activationId !== getLastActivationId() &&
    (next.frames?.length ?? 0) > 0;

  if (isActivation) {
    setLastActivationId(next.activationId!);
    setEnv(next);
    await animateActivation(next);
  } else {
    setEnv(next);
    if (next.snap) emitState(next.snap);
  }

  // Common tail: winner banner or next programming round.
  if (next.snap) {
    if (next.status === "finished" || next.snap.winner != null) {
      if (!getFinishedEmitted()) {
        setFinishedEmitted(true);
        emit({ type: "gameFinished", payload: { winner: next.snap.winner } });
      }
    } else {
      enterRoundIfNeeded(next.snap);
    }
  }
}

async function onRev(): Promise<void> {
  if (getBusy() || getAnimating()) return;
  setBusy(true);
  try {
    const next = await fetchState();
    if (next) await reconcile(next);
  } finally {
    setBusy(false);
  }
}

// ---- lifecycle ---------------------------------------------------------------

function ensureStarted(): void {
  if (started) return;
  const ok = startTransport(onRev);
  if (!ok) return;
  started = true;
  if (getIdentity()?.role === "host") {
    startHostLoop(reconcile);
  }
  void onRev();
}

// ---- public API (unchanged surface) ------------------------------------------

/** Legacy WebSocket accessor. No socket exists now; always null. */
export function getSocket(_reason: string): WebSocket | null {
  return null;
}

export function subscribe(cb: (message: string) => void): () => void {
  const unsubscribe = addListener(cb);
  ensureStarted();
  return unsubscribe;
}

export function sendMessage(data: string | object): boolean {
  let obj: any;
  try {
    obj = typeof data === "string" ? JSON.parse(data) : data;
  } catch {
    return false;
  }
  const type = obj?.payload?.type;
  const id = getIdentity();
  const snap = getEnv()?.snap;

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
      setSubmittedThisRound(true);
      if (id?.role === "host") {
        setHostProgram(cards);
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
  const id = getIdentity();
  const env = getEnv();
  if (!id || !env?.snap) return;
  const r = await postIntent({
    playerIdx: id.seatIdx,
    playerToken: id.playerToken,
    round: env.snap.round,
    type: "program",
    registers: cards,
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
  stopTransport();
  resetHostLoop();
  resetEmit();
  resetStore();
  started = false;
}

export function getQueueSize(): number {
  return 0;
}

/** Current roster (name + robotId) derived from the authoritative envelope.
 *  Used by the board scene in place of the old /api/lobby/getRobot call. */
export function getRoster(): { name: string; robotId: number }[] {
  const players = getEnv()?.players || [];
  return players.map((p) => ({ name: p.name, robotId: p.idx + 1 }));
}

/** This tab's own robotId (seat index + 1), or "" before identity is known. */
export function getMyRobotId(): string {
  const ident = getIdentity() ?? readIdentity();
  return ident ? String(ident.robotId) : "";
}

/** Selected board id for the active game, or null before it is known. */
export function getBoardId(): string | null {
  return getEnv()?.board ?? null;
}

/** Kept so routes that mounted it to keep the connection alive still compile. */
export const WebSocketManager = () => {
  useLocation();
  useEffect(() => {
    ensureStarted();
  }, []);
  return null;
};
