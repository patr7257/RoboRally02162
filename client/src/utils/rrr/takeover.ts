// Host handoff: staleness detection, the takeover election, and demotion
// (issue #4).
//
// The acting host proves it is alive with a heartbeat every 5s (see hostLoop).
// Every player tab watches the hostBeatAt the backend reports on each GET /view
// and, once it is older than STALE_MS on the server clock, declares the host
// dead and schedules a takeover attempt. Attempts are staggered by seat so the
// tabs do not all pile onto POST /resume at once: seat 1 goes immediately, seat
// 2 three seconds later, and so on. The backend is the real arbiter, not the
// stagger: /resume answers 409 "host alive" while the beat is fresh, and a
// successful /resume mints a NEW host token, which fences the old host off with
// a 401 on its next host-authed call. That 401 is what demotes the old tab.
//
// A demoted tab keeps playing as the player of its own seat. The original host
// never posted a seat claim, so it reclaims its seat with {reclaim: true} and
// from then on submits programs as ordinary intents.

import {
  BASE,
  getIdentity,
  jsonFetch,
  refreshIdentity,
  serverNow,
  setVersion,
} from "./transport";
import type { Envelope } from "./transport";
import { getEnv } from "./store";
import { emit } from "./emit";
import {
  beatNow,
  setAutoProgramGhosts,
  setHostProgram,
  setLastReadiness,
  startHostLoop,
  stopHostLoop,
} from "./hostLoop";

/** A host beat older than this (server clock) means the host is gone. Three
 *  missed 5s beats, so a slow network alone does not trigger an election. */
const STALE_MS = 12000;
/** Gap between one seat's takeover attempt and the next seat's. */
const STAGGER_MS = 3000;
/** Pause before re-attempting after a request that simply did not get through. */
const RETRY_MS = 6000;

export interface TakeoverDeps {
  /** ws.ts's reconcile: turns an authoritative envelope into emitted messages. */
  reconcile: (env: Envelope) => Promise<void>;
}

let deps: TakeoverDeps | null = null;
/** The most recent hostBeatAt seen, from any /view response. */
let lastHostBeatAt: number | null = null;
/** Whether this tab currently believes the host is gone. */
let stale = false;
let attemptTimer: ReturnType<typeof setTimeout> | null = null;
let attemptInFlight = false;

export function initTakeover(d: TakeoverDeps): void {
  deps = d;
}

// ---- staleness --------------------------------------------------------------

function beatIsStale(beatAt: number | null): boolean {
  if (getEnv()?.status !== "active") return false;
  if (beatAt == null) return false;
  return serverNow() - beatAt > STALE_MS;
}

function cancelAttempt(): void {
  if (attemptTimer) clearTimeout(attemptTimer);
  attemptTimer = null;
}

/**
 * Called with the hostBeatAt of every /view response, including the
 * `unchanged: true` ones: a dead host is precisely the case where no new
 * revision ever arrives, so the short response is the only signal there is.
 */
export function noteHostBeat(beatAt: number | null): void {
  if (beatAt != null) lastHostBeatAt = beatAt;
  const id = getIdentity();
  if (!id || id.role === "host") return; // the host does not elect itself

  const isStale = beatIsStale(lastHostBeatAt);
  if (isStale && !stale) {
    stale = true;
    emit({ type: "hostStale", payload: {} });
    scheduleAttempt(id.seatIdx);
    return;
  }
  if (!isStale && stale) {
    clearStale();
    emit({ type: "hostRecovered", payload: {} });
  }
}

function clearStale(): void {
  stale = false;
  cancelAttempt();
}

// ---- election ---------------------------------------------------------------

/** Seat 1 attempts immediately, every later seat waits its turn, so the lowest
 *  surviving seat normally wins without any tab having to coordinate. */
function scheduleAttempt(seatIdx: number): void {
  scheduleIn(Math.max(0, (seatIdx - 1) * STAGGER_MS));
}

function scheduleIn(delay: number): void {
  cancelAttempt();
  attemptTimer = setTimeout(() => {
    attemptTimer = null;
    void runAttempt();
  }, delay);
}

/** One scheduled attempt, plus a retry when it neither won nor found a live
 *  host: a dropped request must not leave the game leaderless forever. */
async function runAttempt(): Promise<void> {
  await attemptTakeover();
  if (stale && getIdentity()?.role !== "host") scheduleIn(RETRY_MS);
}

/**
 * Claims the host role through POST /resume. Re-checks the beat first: by the
 * time a late seat's turn comes round the host may be back, or an earlier seat
 * may already have taken over and started beating. A 409 is the backend saying
 * it still sees a live host, and the backend is the authority on that, so the
 * election is called off rather than retried.
 */
export async function attemptTakeover(): Promise<void> {
  if (attemptInFlight) return;
  const id = getIdentity();
  if (!id || id.role === "host") return;
  if (!beatIsStale(lastHostBeatAt)) {
    clearStale();
    return;
  }
  attemptInFlight = true;
  try {
    const r = await jsonFetch(`${BASE}/games/${id.gameId}/resume`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password: id.pw }),
    });
    if (r.status === 409) {
      clearStale();
      emit({ type: "hostRecovered", payload: {} });
      return;
    }
    if (!r.ok || !r.data?.hostToken) return;
    await promoteToHost(r.data.hostToken, r.data.version, r.data.state as Envelope);
  } finally {
    attemptInFlight = false;
  }
}

/** Adopts the host role: the freshly minted token, the version the resume
 *  returned, the unredacted state, and the host loop. The seat index is kept:
 *  this tab still plays its own robot, it just also drives the game now. */
async function promoteToHost(
  hostToken: string,
  version: number,
  state: Envelope,
): Promise<void> {
  sessionStorage.setItem("rrr_role", "host");
  sessionStorage.setItem("rrr_hostToken", hostToken);
  refreshIdentity();
  clearStale();
  lastHostBeatAt = null;

  setVersion(version);
  // The dead host's own unsubmitted program came back unredacted; it is not ours
  // and must not be replayed as this seat's pick.
  setHostProgram(null);
  setLastReadiness({ ...(state.readiness ?? {}) });
  // Seats with no claim can never answer again, so stop waiting on them.
  setAutoProgramGhosts(true);

  emit({ type: "hostChanged", payload: {} });
  startHostLoop((fresh) => void deps?.reconcile(fresh));
  beatNow();
  if (deps) await deps.reconcile(state);
}

// ---- demotion ---------------------------------------------------------------

/** Reads (and lazily creates) this browser's stable device id, the same key the
 *  create/join lobby uses when it claims a seat. */
function deviceId(): string {
  let d = localStorage.getItem("rrr_device");
  if (!d) {
    d = "d" + Math.random().toString(36).slice(2) + Date.now().toString(36);
    localStorage.setItem("rrr_device", d);
  }
  return d;
}

/**
 * A host-authed call came back 401: another tab won an election and this token
 * has been fenced off. Step down to player and keep playing this seat's robot.
 */
export function handleUnauthorized(): void {
  const id = getIdentity();
  if (!id || id.role !== "host") return;
  stopHostLoop();
  sessionStorage.removeItem("rrr_hostToken");
  sessionStorage.setItem("rrr_role", "player");
  refreshIdentity();
  clearStale();
  lastHostBeatAt = null;
  setAutoProgramGhosts(false);
  emit({ type: "hostDemoted", payload: {} });
  void ensureSeatClaimed();
}

/** The original host answered its own prompts through the host loop and never
 *  posted a seat claim, so it has no player token to submit intents with. Claim
 *  the seat it has been playing all along; the backend allows an empty seat to
 *  be reclaimed mid-game on the right password. */
async function ensureSeatClaimed(): Promise<void> {
  const id = getIdentity();
  if (!id || id.playerToken) return;
  const r = await jsonFetch(`${BASE}/games/${id.gameId}/seats`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      playerIdx: id.seatIdx,
      deviceId: deviceId(),
      name: id.name,
      pw: id.pw,
      reclaim: true,
    }),
  });
  if (r.ok && r.data?.playerToken) {
    sessionStorage.setItem("rrr_playerToken", r.data.playerToken);
    refreshIdentity();
  } else {
    emit({
      type: "error",
      payload: { message: "Could not reclaim your seat; you are a spectator." },
    });
  }
}

// ---- lifecycle ---------------------------------------------------------------

export function reset(): void {
  cancelAttempt();
  deps = null;
  lastHostBeatAt = null;
  stale = false;
  attemptInFlight = false;
}
