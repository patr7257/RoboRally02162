// Shared session state read/written by reconciliation (ws.ts), playback
// (emit.ts), and the host loop (hostLoop.ts). A thin getter/setter module so
// each concern can import only the slice it needs without reaching into
// another module's closure.

import type { Envelope } from "./transport";

let env: Envelope | null = null;
let busy = false;
let animating = false;
let lastActivationId = -1;
let lastRoundEntered = 0;
let finishedEmitted = false;

export function getEnv(): Envelope | null {
  return env;
}
export function setEnv(v: Envelope | null): void {
  env = v;
}

export function getBusy(): boolean {
  return busy;
}
export function setBusy(v: boolean): void {
  busy = v;
}

export function getAnimating(): boolean {
  return animating;
}
export function setAnimating(v: boolean): void {
  animating = v;
}

export function getLastActivationId(): number {
  return lastActivationId;
}
export function setLastActivationId(v: number): void {
  lastActivationId = v;
}

export function getLastRoundEntered(): number {
  return lastRoundEntered;
}
export function setLastRoundEntered(v: number): void {
  lastRoundEntered = v;
}

export function getFinishedEmitted(): boolean {
  return finishedEmitted;
}
export function setFinishedEmitted(v: boolean): void {
  finishedEmitted = v;
}

/** Resets exactly the slice closeSocket used to reset (busy/animating are
 *  left alone, matching the old behavior). */
export function reset(): void {
  env = null;
  lastActivationId = -1;
  lastRoundEntered = 0;
  finishedEmitted = false;
}
