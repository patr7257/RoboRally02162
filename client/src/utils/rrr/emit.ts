// Fan-out to Board.tsx's message listeners. Synthesizes the exact JSON-string
// messages the old WebSocket transport produced (stateSnapshot / hand /
// discard / damageDecks / programmingStarted / roundExecuting / readiness /
// lastMoves), the reaction and respawn prompts, and the activation playback
// animation (one frame every FRAME_MS).

import type { GameSnapshot } from "../../engine/roborally-engine";
import {
  snapshotToGameData,
  frameToGameData,
  handForRobot,
  discardForRobot,
} from "../engineAdapter";
import { getIdentity, readIdentity } from "./transport";
import type { Envelope, Identity } from "./transport";
import { getEnv, setAnimating, getLastRoundEntered, setLastRoundEntered } from "./store";
import {
  getLastReadiness,
  getSubmittedThisRound,
  setSubmittedThisRound,
  setHostProgram,
} from "./hostLoop";

const FRAME_MS = 320; // slightly above BoardRenderer's 0.3s CSS transition
const FAKE_MS_REMAINING = 999000; // no server programming timer; disables auto-submit

let listeners: Set<(message: string) => void> = new Set();
/** The prompt this tab has already opened a modal for, so SSE pings and
 *  fallback polls of the same paused snapshot do not re-open it. */
let lastPromptKey: string | null = null;
/** The round whose program rejection has already been surfaced here. */
let rejectionRoundEmitted = -1;
/** Per-round move log (issue #7), accumulated across the segments of a round. */
let lastMoves: { robotId: number; move: string }[] = [];
/** Snapshot status of the previously animated activation: a segment that
 *  followed a pause continues the same round, anything else starts a new one. */
let lastAnimatedStatus: string | null = null;

/** Registers a listener and returns an unsubscribe function. */
export function addListener(cb: (message: string) => void): () => void {
  listeners.add(cb);
  return () => {
    listeners.delete(cb);
  };
}

export function emit(obj: unknown): void {
  const s = JSON.stringify(obj);
  listeners.forEach((cb) => cb(s));
}

function identity(): Identity | null {
  return getIdentity() ?? readIdentity();
}

export function emitState(snap: GameSnapshot): void {
  emit({ type: "stateSnapshot", payload: snapshotToGameData(snap) });
}

export function emitHandBundle(snap: GameSnapshot): void {
  const id = getIdentity();
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

/**
 * Opens a new programming round exactly once per round. The host seeds its own
 * program state from the persisted hostPrivate blob when the reloaded tab lands
 * back in the round it was already programming (issue #3); every other case
 * starts from a clean slate.
 */
export function enterRoundIfNeeded(snap: GameSnapshot): void {
  if (snap.status !== "programming") return;
  if (snap.round === getLastRoundEntered()) return;
  setLastRoundEntered(snap.round);
  const id = getIdentity();
  const hostPrivate = getEnv()?.hostPrivate;
  if (id?.role === "host" && hostPrivate && hostPrivate.round === snap.round) {
    setHostProgram(hostPrivate.hostProgram);
    setSubmittedThisRound(hostPrivate.submittedThisRound);
  } else {
    setSubmittedThisRound(false);
    setHostProgram(null);
  }
  emit({ type: "programmingStarted" });
  emitHandBundle(snap);
}

export function emitReadiness(): void {
  const id = getIdentity();
  const env = getEnv();
  const playerSubmitted: Record<number, boolean> = {};
  if (id?.role === "host") {
    Object.entries(getLastReadiness()).forEach(([idx, v]) => {
      playerSubmitted[Number(idx) + 1] = v; // key by robotId
    });
  } else if (env?.readiness) {
    Object.entries(env.readiness).forEach(([idx, v]) => {
      playerSubmitted[Number(idx) + 1] = v;
    });
  } else if (id) {
    playerSubmitted[id.robotId] = getSubmittedThisRound();
  }
  emit({
    type: "readiness",
    payload: { playerSubmitted, msRemaining: FAKE_MS_REMAINING },
  });
}

/** The current round's move log, for an explicit getLastMoves request. */
export function getLastMoves(): { robotId: number; move: string }[] {
  return [...lastMoves];
}

export function emitLastMoves(): void {
  emit({ type: "lastMoves", payload: { moves: getLastMoves() } });
}

export function animateActivation(next: Envelope): Promise<void> {
  return new Promise((resolve) => {
    const snap = next.snap!;
    const frames = next.frames || [];
    const continuesRound =
      lastAnimatedStatus === "awaiting-reaction" ||
      lastAnimatedStatus === "awaiting-respawn";
    if (!continuesRound) lastMoves = [];
    lastAnimatedStatus = snap.status;
    setAnimating(true);
    emit({ type: "roundExecuting" });

    let i = 0;
    const step = () => {
      if (i < frames.length) {
        const frame = frames[i];
        emit({ type: "stateSnapshot", payload: frameToGameData(snap, frame) });
        const label = frame.label;
        if (label && label.robotId != null) {
          lastMoves.push({ robotId: label.robotId, move: label.text });
          emitLastMoves();
        }
        i++;
        setTimeout(step, FRAME_MS);
      } else {
        emitState(snap); // settle on authoritative positions
        setAnimating(false);
        resolve();
      }
    };
    step();
  });
}

/**
 * Turns a paused authoritative snapshot into this tab's modal prompts, and
 * surfaces a program the host had to throw away. Called from reconcile's common
 * tail, so a prompt only opens once the activation animation has settled.
 * Every tab only ever prompts for its own robot (issues #5, #6, #14).
 */
export function syncPrompts(env: Envelope): void {
  const snap = env.snap;
  const id = identity();
  if (!snap || !id) return;

  if (snap.status === "awaiting-reaction" && snap.pendingReaction) {
    const pending = snap.pendingReaction;
    if (pending.robotId === id.robotId && lastPromptKey !== pending.promptId) {
      lastPromptKey = pending.promptId;
      emit({
        type: "reactionNeeded",
        payload: {
          kind: pending.kind,
          options: pending.options,
          deadline: env.deadlineAt ?? null,
          promptId: pending.promptId,
        },
      });
    }
  } else if (snap.status === "awaiting-respawn") {
    const key = "respawn-r" + snap.round;
    if (lastPromptKey !== key) {
      snap.robots
        .filter((r) => !r.alive && r.id === id.robotId)
        .forEach((r) => {
          lastPromptKey = key;
          emit({ type: "needRespawnDirection", payload: { robotId: r.id } });
        });
    }
  }

  const rejection = env.rejections?.[id.seatIdx];
  if (
    rejection &&
    rejection.round === snap.round &&
    rejectionRoundEmitted !== snap.round
  ) {
    rejectionRoundEmitted = snap.round;
    emit({
      type: "error",
      payload: {
        message:
          "Your submitted program was invalid and was auto-replaced: " +
          rejection.reason,
      },
    });
  }
}

/** Clears listener bookkeeping and the per-tab prompt / move-log state,
 *  matching what closeSocket did before. */
export function reset(): void {
  listeners.clear();
  lastPromptKey = null;
  rejectionRoundEmitted = -1;
  lastMoves = [];
  lastAnimatedStatus = null;
}
