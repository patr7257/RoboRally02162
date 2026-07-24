// Fan-out to Board.tsx's message listeners. Synthesizes the exact JSON-string
// messages the old WebSocket transport produced (stateSnapshot / hand /
// discard / damageDecks / programmingStarted / roundExecuting / readiness),
// plus the activation playback animation (one frame every FRAME_MS).

import type { GameSnapshot } from "../../engine/roborally-engine";
import {
  snapshotToGameData,
  frameToGameData,
  handForRobot,
  discardForRobot,
} from "../engineAdapter";
import { getIdentity } from "./transport";
import type { Envelope } from "./transport";
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

export function enterRoundIfNeeded(snap: GameSnapshot): void {
  if (snap.status !== "programming") return;
  if (snap.round === getLastRoundEntered()) return;
  setLastRoundEntered(snap.round);
  setSubmittedThisRound(false);
  setHostProgram(null);
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

export function animateActivation(next: Envelope): Promise<void> {
  return new Promise((resolve) => {
    const snap = next.snap!;
    const frames = next.frames || [];
    setAnimating(true);
    emit({ type: "roundExecuting" });

    let i = 0;
    const step = () => {
      if (i < frames.length) {
        emit({ type: "stateSnapshot", payload: frameToGameData(snap, frames[i]) });
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

/** Clears listener bookkeeping, matching what closeSocket did before. */
export function reset(): void {
  listeners.clear();
}
