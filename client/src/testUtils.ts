// Shared sessionStorage seeding helpers for specs. Kept separate from
// setupTests.ts so individual test files can import only what they need.
// Mirrors the keys client/src/utils/ws.ts (readIdentity) and
// client/src/board/Board.tsx read from sessionStorage.

export interface SeedIdentityOptions {
  gameId: string;
  role?: "host" | "player";
  seatIdx?: number;
  name?: string;
  pw?: string;
  hostToken?: string | null;
  playerToken?: string | null;
}

/** Seeds the rrr_* sessionStorage keys that ws.ts's readIdentity() expects. */
export function seedIdentity(opts: SeedIdentityOptions): void {
  const seatIdx = opts.seatIdx ?? 0;
  sessionStorage.setItem("rrr_gameId", opts.gameId);
  sessionStorage.setItem("rrr_role", opts.role ?? "player");
  sessionStorage.setItem("rrr_seatIdx", String(seatIdx));
  sessionStorage.setItem("rrr_name", opts.name ?? `Player ${seatIdx + 1}`);
  sessionStorage.setItem("rrr_pw", opts.pw ?? "");
  if (opts.hostToken) sessionStorage.setItem("rrr_hostToken", opts.hostToken);
  if (opts.playerToken) {
    sessionStorage.setItem("rrr_playerToken", opts.playerToken);
  }
}

export interface SeedLegacyBoardIdentityOptions {
  gameId: string;
  username?: string;
  robotId?: number;
  mode?: "demo" | "normal";
}

/** Seeds the legacy id/username/robotID/mode keys client/src/board/Board.tsx reads. */
export function seedLegacyBoardIdentity(
  opts: SeedLegacyBoardIdentityOptions,
): void {
  sessionStorage.setItem("id", opts.gameId);
  sessionStorage.setItem("username", opts.username ?? "");
  sessionStorage.setItem("robotID", String(opts.robotId ?? 1));
  sessionStorage.setItem("mode", opts.mode ?? "normal");
}

/** Clears all sessionStorage state (identity and anything else a test wrote). */
export function clearIdentity(): void {
  sessionStorage.clear();
}
