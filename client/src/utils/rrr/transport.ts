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

export const BASE = "/api/robot-rally";
const FALLBACK_POLL_MS = 3000;

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

// ---- module state ---------------------------------------------------------

let id: Identity | null = null;
let version = 0;
let es: EventSource | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;

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

/** Fetch the latest authoritative blob. Returns the new envelope, or null if
 *  unchanged / unavailable. */
export async function fetchState(): Promise<Envelope | null> {
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

/** Host-only optimistic-concurrency write. Returns true on success. On a 409
 *  (lost the race) it refetches the fresh state and, if given, hands it to
 *  onConflict so the caller can reconcile without this module knowing what
 *  reconciliation means. */
export async function putState(
  next: Envelope,
  onConflict?: (fresh: Envelope) => void,
): Promise<boolean> {
  if (!id) return false;
  const r = await jsonFetch(`${BASE}/games/${id.gameId}/state`, {
    method: "PUT",
    headers: hostHeaders(),
    body: JSON.stringify({ baseVersion: version, state: next }),
  });
  if (r.ok && r.data) {
    version = r.data.version;
    return true;
  }
  if (r.status === 409) {
    const fresh = await fetchState();
    if (fresh && onConflict) onConflict(fresh);
  }
  return false;
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
}
