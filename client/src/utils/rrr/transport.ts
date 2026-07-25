// Fetch + SSE transport for the Vercel-backed RoboRally game state
// (/api/robot-rally/*). Owns identity (read from sessionStorage, written by
// the create/join lobby), the optimistic-concurrency version counter, and
// the inbound SSE/fallback-poll lifecycle. Nothing here knows about game
// rules or how inbound revisions get turned into UI messages; callers pass
// an onRev callback and, for host writes, an onConflict callback.
//
// Identity is read from sessionStorage (written by the create/join lobby):
//   rrr_gameId, rrr_role ("host"|"player"), rrr_hostToken, rrr_playerToken,
//   rrr_pw, rrr_seatIdx, rrr_name.

import type { GameSnapshot, Frame } from "../../engine/roborally-engine";
import { MoveType } from "../../types/boardTypes";

export const BASE = "/api/robot-rally";
const FALLBACK_POLL_MS = 3000;

/** The host tab's own unsubmitted programming state, persisted so a reloaded
 *  host tab can resume the round it was in the middle of (issue #3). The
 *  backend redacts this key from the player-facing GET /view, so it never
 *  leaks a program to opponents. */
export interface HostPrivate {
  round: number;
  hostProgram: MoveType[] | null;
  submittedThisRound: boolean;
}

/** Why a player's submitted program was thrown away, keyed by seat index. The
 *  round is the round of the snapshot the rejection is published with, so the
 *  owning tab can surface it once and ignore it afterwards (issue #14). */
export interface Rejection {
  round: number;
  reason: string;
}

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
  /** Host-only, redacted from GET /view. */
  hostPrivate?: HostPrivate;
  /** When the host stops waiting for a reaction / respawn choice, or (issue #9)
   *  when the programming phase auto-activates. Always server-clock based. */
  deadlineAt?: number | null;
  rejections?: Record<number, Rejection>;
  /** Programming-phase time limit in ms, chosen at create time (issue #9).
   *  Absent means no timer: programming waits for everyone, as it always did. */
  timerMs?: number;
  // server-managed: gameId, version, createdAt, updatedAt
  /** Server write timestamp, echoed inside the state blob. Read-only here; used
   *  to keep this tab's clock skew against the server (issue #4). */
  updatedAt?: number;
}

export interface Identity {
  gameId: string;
  role: "host" | "player";
  hostToken: string | null;
  playerToken: string | null;
  pw: string;
  seatIdx: number;
  robotId: number;
  name: string;
}

/** What one authoritative read produced: the new envelope (null when nothing
 *  changed or the read failed) plus the host liveness beat, which the backend
 *  reports on every GET /view, including the `unchanged: true` short response.
 *  Player tabs need the beat even when the revision did not move: a dead host is
 *  exactly the case where no new revision ever arrives (issue #4). */
export interface FetchResult {
  env: Envelope | null;
  hostBeatAt: number | null;
}

// ---- module state ---------------------------------------------------------

let id: Identity | null = null;
let version = 0;
let es: EventSource | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;
/** Date.now() - server clock, from the last full state read. Deadlines are
 *  written and compared on the server clock so every tab agrees (issue #4). */
let clockSkewMs = 0;
let onUnauthorized: (() => void) | null = null;

// ---- identity ---------------------------------------------------------------

export function readIdentity(): Identity | null {
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

export function getIdentity(): Identity | null {
  return id;
}

/** Re-reads sessionStorage into the live identity. Used by the host handoff
 *  paths (promotion and demotion rewrite rrr_role / rrr_hostToken). */
export function refreshIdentity(): Identity | null {
  id = readIdentity();
  return id;
}

export function getVersion(): number {
  return version;
}

export function setVersion(v: number): void {
  version = v;
}

export function hostHeaders(): Record<string, string> {
  return {
    "Content-Type": "application/json",
    "x-rrr-host-token": id?.hostToken || "",
  };
}

// ---- clock ------------------------------------------------------------------

/** Best estimate of the server's current epoch ms. Falls back to the local
 *  clock until the first authoritative read lands. */
export function serverNow(): number {
  return Date.now() - clockSkewMs;
}

/** The updatedAt sample the current skew was computed from. Skew must only be
 *  resampled when the server actually wrote (updatedAt changed): updatedAt is
 *  frozen between writes, so resampling on every read would grow the "skew" at
 *  the speed of real time and pin serverNow() to the last write forever. */
let skewSampleAt: number | null = null;

function noteServerClock(state: Envelope | null | undefined): void {
  if (
    state &&
    typeof state.updatedAt === "number" &&
    state.updatedAt !== skewSampleAt
  ) {
    skewSampleAt = state.updatedAt;
    clockSkewMs = Date.now() - state.updatedAt;
  }
}

// ---- host-token fencing -----------------------------------------------------

/** Registers the handler invoked when a host-authed call comes back 401, i.e.
 *  another tab took the host role over and this tab's token was fenced off. */
export function setUnauthorizedHandler(fn: (() => void) | null): void {
  onUnauthorized = fn;
}

// ---- fetch helpers ----------------------------------------------------------

export async function jsonFetch(url: string, opts?: RequestInit) {
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

/** Any call carrying the host token. Injects the host headers and funnels a 401
 *  into the demotion handler: a 401 means a takeover minted a new host token and
 *  fenced this one off (issue #4). */
export async function hostFetch(url: string, opts: RequestInit = {}) {
  const r = await jsonFetch(url, {
    ...opts,
    headers: { ...hostHeaders(), ...(opts.headers || {}) },
  });
  if (r.status === 401 && onUnauthorized) onUnauthorized();
  return r;
}

/** Fetch the latest authoritative blob. `env` is the new envelope, or null when
 *  unchanged / unavailable; `hostBeatAt` is the host's last liveness beat as
 *  reported to player tabs (null for the host's own read). */
export async function fetchState(): Promise<FetchResult> {
  if (!id) return { env: null, hostBeatAt: null };
  if (id.role === "host") {
    const r = await hostFetch(`${BASE}/games/${id.gameId}/state`);
    if (!r.ok || !r.data) return { env: null, hostBeatAt: null };
    version = r.data.version;
    const state = r.data.state as Envelope;
    noteServerClock(state);
    return { env: state, hostBeatAt: null };
  }
  const r = await jsonFetch(
    `${BASE}/games/${id.gameId}/view?pw=${encodeURIComponent(id.pw)}&v=${version}`,
  );
  if (!r.ok || !r.data) return { env: null, hostBeatAt: null };
  const hostBeatAt =
    typeof r.data.hostBeatAt === "number" ? (r.data.hostBeatAt as number) : null;
  if (r.data.unchanged) return { env: null, hostBeatAt };
  version = r.data.version;
  const state = r.data.state as Envelope;
  noteServerClock(state);
  return { env: state, hostBeatAt };
}

/** Host-only optimistic-concurrency write. Returns true on success. On a 409
 *  (lost the race) it refetches the fresh state and, if given, hands it to
 *  onConflict so the caller can reconcile without this module knowing what
 *  reconciliation means. */
export async function putState(
  next: Envelope,
  onConflict?: (fresh: Envelope) => void,
): Promise<boolean> {
  if (!id) return false;
  const r = await hostFetch(`${BASE}/games/${id.gameId}/state`, {
    method: "PUT",
    body: JSON.stringify({ baseVersion: version, state: next }),
  });
  if (r.ok && r.data) {
    version = r.data.version;
    return true;
  }
  if (r.status === 409) {
    const fresh = await fetchState();
    if (fresh.env && onConflict) onConflict(fresh.env);
  }
  return false;
}

/** Fire-and-forget host liveness beat. Never bumps the revision, never blocks
 *  the caller, and a 401 demotes this tab through the unauthorized handler. */
export function sendHeartbeat(): void {
  if (!id || id.role !== "host") return;
  void hostFetch(`${BASE}/games/${id.gameId}/heartbeat`, { method: "POST" });
}

/** POST a player intent (e.g. a submitted program) for the current game. */
export async function postIntent(body: object) {
  if (!id) return { ok: false, status: 0, data: null };
  return jsonFetch(`${BASE}/games/${id.gameId}/intent`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

// ---- SSE + fallback-poll lifecycle ------------------------------------------

/** Reads identity, opens the SSE stream, and starts the fallback poll timer.
 *  Returns false (no-op) if no identity is available yet. `onRev` is called
 *  on every SSE message and every fallback poll tick. */
export function startTransport(onRev: () => void | Promise<void>): boolean {
  id = readIdentity();
  if (!id) return false;

  es = new EventSource(`${BASE}/games/${id.gameId}/events`);
  es.onmessage = () => {
    void onRev();
  };
  es.onerror = () => {
    /* EventSource auto-reconnects; the fallback poll covers gaps */
  };
  pollTimer = setInterval(() => void onRev(), FALLBACK_POLL_MS);
  return true;
}

export function stopTransport(): void {
  if (es) es.close();
  es = null;
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = null;
  version = 0;
  clockSkewMs = 0;
  skewSampleAt = null;
  onUnauthorized = null;
}
