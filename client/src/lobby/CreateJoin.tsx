import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { parseBoardDefinition, createGame } from "../engine/roborally-engine";
import { BASE, Envelope } from "../utils/ws";
import { ROBOT_COLORS } from "../types/constants";
import { BOARD_CATALOG, DEFAULT_BOARD_ID } from "./boardCatalog";
import { buildPlayerConfigs } from "../utils/gameSetup";
import "../styles/lobby.css";

/**
 * Single-screen create/join lobby for the same-origin Vercel backend. Replaces
 * the Java-gateway auth + multi-scene lobby. The creator becomes the
 * authoritative host (seat 0); joiners claim the lowest free seat via password.
 * When the host starts, the board definition is loaded, createGame builds the
 * initial snapshot, and it is PUT as the active game state, after which every
 * tab navigates into the board scene.
 */

const MAX_PLAYERS = 6;
// Player display name is capped at the backend MAX_NAME_LENGTH (24); the game
// name may be up to 40 (validated by the games create route).
const NAME_RE = /^.{1,24}$/;
const GAME_NAME_RE = /^.{1,40}$/;
const CODE_RE = /^[A-Z0-9]{4,8}$/;
const PW_RE = /^[\x20-\x7e]{4,32}$/;

/** Optional programming-phase time limit (issue #9). Off is the default: the
 *  round then waits for every seat, exactly as it did before the timer existed. */
const TIMER_OPTIONS: { label: string; ms: number }[] = [
  { label: "Off", ms: 0 },
  { label: "30s", ms: 30000 },
  { label: "60s", ms: 60000 },
  { label: "90s", ms: 90000 },
  { label: "120s", ms: 120000 },
];

function deviceId(): string {
  let d = localStorage.getItem("rrr_device");
  if (!d) {
    d = "d" + Math.random().toString(36).slice(2) + Date.now().toString(36);
    localStorage.setItem("rrr_device", d);
  }
  return d;
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
  } catch {
    return { ok: false, status: 0, data: null as any };
  }
}

function storeIdentity(opts: {
  gameId: string;
  role: "host" | "player";
  hostToken?: string;
  playerToken?: string;
  pw: string;
  seatIdx: number;
  name: string;
}) {
  sessionStorage.setItem("rrr_gameId", opts.gameId);
  sessionStorage.setItem("rrr_role", opts.role);
  sessionStorage.setItem("rrr_pw", opts.pw);
  sessionStorage.setItem("rrr_seatIdx", String(opts.seatIdx));
  sessionStorage.setItem("rrr_name", opts.name);
  if (opts.hostToken) sessionStorage.setItem("rrr_hostToken", opts.hostToken);
  if (opts.playerToken) sessionStorage.setItem("rrr_playerToken", opts.playerToken);
  // Legacy keys Board.tsx still reads.
  sessionStorage.setItem("id", opts.gameId);
  sessionStorage.setItem("username", opts.name);
  sessionStorage.setItem("robotID", String(opts.seatIdx + 1));
  sessionStorage.setItem("mode", "normal");
}

export default function CreateJoin() {
  const navigate = useNavigate();
  const [phase, setPhase] = useState<"home" | "lobby">("home");
  const [role, setRole] = useState<"host" | "player">("player");
  const [error, setError] = useState<string>("");
  const [busy, setBusy] = useState<boolean>(false);

  // Home form fields.
  const [name, setName] = useState<string>(sessionStorage.getItem("rrr_name") || "");
  const [gameName, setGameName] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [joinCode, setJoinCode] = useState<string>("");
  const [selectedBoardId, setSelectedBoardId] = useState<string>(DEFAULT_BOARD_ID);
  const [timerMs, setTimerMs] = useState<number>(0);

  // Lobby state.
  const [gameId, setGameId] = useState<string>("");
  const [pw, setPw] = useState<string>("");
  const [players, setPlayers] = useState<{ idx: number; name: string }[]>([]);

  const esRef = useRef<EventSource | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const cleanup = () => {
    if (esRef.current) esRef.current.close();
    esRef.current = null;
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = null;
  };
  useEffect(() => cleanup, []);

  // ---- create -----------------------------------------------------------

  const onCreate = async () => {
    setError("");
    if (!NAME_RE.test(name)) return setError("Enter your name (1-24 characters).");
    if (!GAME_NAME_RE.test(gameName)) return setError("Enter a game name (1-40 characters).");
    if (!PW_RE.test(password)) return setError("Password must be 4-32 characters.");
    setBusy(true);
    const initial: Envelope = {
      name: gameName,
      status: "lobby",
      phase: "lobby",
      round: 0,
      current: 0,
      players: [{ idx: 0, name, color: ROBOT_COLORS[0] }],
      board: selectedBoardId,
      // Left off the envelope entirely when the timer is Off, so the host loop
      // sees no timer at all rather than a zero-length one.
      ...(timerMs > 0 ? { timerMs } : {}),
    };
    const r = await jsonFetch(`${BASE}/games`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: gameName, password, state: initial }),
    });
    setBusy(false);
    if (!r.ok || !r.data?.gameId) return setError("Could not create game.");
    storeIdentity({
      gameId: r.data.gameId,
      role: "host",
      hostToken: r.data.hostToken,
      pw: password,
      seatIdx: 0,
      name,
    });
    setRole("host");
    setGameId(r.data.gameId);
    setPw(password);
    setPlayers(initial.players.map((p) => ({ idx: p.idx, name: p.name })));
    setPhase("lobby");
  };

  // ---- join (claim lowest free seat) ------------------------------------

  const claimAnySeat = async (
    code: string,
    pwd: string,
    playerName: string,
    idx: number,
  ): Promise<{ idx: number; playerToken: string } | null> => {
    if (idx >= MAX_PLAYERS) {
      setError("Game is full.");
      return null;
    }
    const r = await jsonFetch(`${BASE}/games/${code}/seats`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ playerIdx: idx, deviceId: deviceId(), name: playerName, pw: pwd }),
    });
    if (r.ok && r.data?.playerToken) return { idx, playerToken: r.data.playerToken };
    if (r.status === 403) {
      setError("Wrong password.");
      return null;
    }
    if (r.status === 404) {
      setError("Game not found.");
      return null;
    }
    if (r.status === 409) {
      if (r.data?.error === "already started") {
        setError("This game has already started.");
        return null;
      }
      return claimAnySeat(code, pwd, playerName, idx + 1);
    }
    setError("Could not join game.");
    return null;
  };

  const onJoin = async () => {
    setError("");
    if (!NAME_RE.test(name)) return setError("Enter your name (1-24 characters).");
    const code = joinCode.toUpperCase();
    if (!CODE_RE.test(code)) return setError("Enter a valid game code.");
    if (!PW_RE.test(password)) return setError("Password must be 4-32 characters.");
    setBusy(true);
    const res = await claimAnySeat(code, password, name, 1);
    setBusy(false);
    if (!res) return;
    storeIdentity({
      gameId: code,
      role: "player",
      playerToken: res.playerToken,
      pw: password,
      seatIdx: res.idx,
      name,
    });
    setRole("player");
    setGameId(code);
    setPw(password);
    setPhase("lobby");
  };

  // ---- lobby loop -------------------------------------------------------

  useEffect(() => {
    if (phase !== "lobby" || !gameId) return;

    const hostHeaders = { "x-rrr-host-token": sessionStorage.getItem("rrr_hostToken") || "" };

    const tick = async () => {
      if (role === "host") {
        const s = await jsonFetch(`${BASE}/games/${gameId}/state`, { headers: hostHeaders });
        if (!s.ok || !s.data?.state) return;
        const state: Envelope = s.data.state;
        let version: number = s.data.version;
        // Discover claimed seats and fold new ones into the roster.
        const it = await jsonFetch(`${BASE}/games/${gameId}/intents?round=0`, {
          headers: hostHeaders,
        });
        const seats: Record<string, any> = it.ok ? it.data?.seats || {} : {};
        const known = new Set(state.players.map((p) => p.idx));
        let changed = false;
        const merged = [...state.players];
        Object.entries(seats).forEach(([idxStr, info]) => {
          const idx = Number(idxStr);
          if (!known.has(idx)) {
            merged.push({ idx, name: info?.name || `Player ${idx + 1}`, color: ROBOT_COLORS[idx % ROBOT_COLORS.length] });
            changed = true;
          }
        });
        if (changed) {
          merged.sort((a, b) => a.idx - b.idx);
          const put = await jsonFetch(`${BASE}/games/${gameId}/state`, {
            method: "PUT",
            headers: { "Content-Type": "application/json", ...hostHeaders },
            body: JSON.stringify({ baseVersion: version, state: { ...state, players: merged } }),
          });
          if (put.ok) version = put.data.version;
        }
        setPlayers(merged.map((p) => ({ idx: p.idx, name: p.name })));
      } else {
        const v = await jsonFetch(
          `${BASE}/games/${gameId}/view?pw=${encodeURIComponent(pw)}`,
        );
        if (!v.ok || !v.data?.state) return;
        const state: Envelope = v.data.state;
        setPlayers(state.players.map((p) => ({ idx: p.idx, name: p.name })));
        if (state.status === "active") {
          cleanup();
          navigate("/boardScene");
        }
      }
    };

    esRef.current = new EventSource(`${BASE}/games/${gameId}/events`);
    esRef.current.onmessage = () => void tick();
    pollRef.current = setInterval(() => void tick(), 2000);
    void tick();
    return cleanup;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, gameId, role, pw]);

  // ---- host start -------------------------------------------------------

  const onStart = async () => {
    setError("");
    setBusy(true);
    const hostHeaders = { "x-rrr-host-token": sessionStorage.getItem("rrr_hostToken") || "" };
    try {
      const s = await jsonFetch(`${BASE}/games/${gameId}/state`, { headers: hostHeaders });
      if (!s.ok || !s.data?.state) throw new Error("state");
      const state: Envelope = s.data.state;
      const version: number = s.data.version;

      const boardUrl = state.board
        ? `${process.env.PUBLIC_URL}/boards/${state.board}.json`
        : `${process.env.PUBLIC_URL}/board.json`;
      let boardRes = await fetch(boardUrl);
      if (!boardRes.ok) boardRes = await fetch(`${process.env.PUBLIC_URL}/board.json`);
      const def = await boardRes.json();
      const loaded = parseBoardDefinition(def);

      const configs = buildPlayerConfigs(loaded, state.players);

      const snap = createGame(loaded.board, configs);
      const next: Envelope = {
        ...state,
        status: "active",
        phase: "programming",
        round: 1,
        current: 0,
        /* Keep the board ID (filename stem), not the display name: every tab
           re-fetches boards/<id>.json for the banner and starting-area shading. */
        board: state.board ?? loaded.displayName,
        snap,
        frames: [],
        activationId: 0,
        readiness: {},
      };
      const put = await jsonFetch(`${BASE}/games/${gameId}/state`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...hostHeaders },
        body: JSON.stringify({ baseVersion: version, state: next }),
      });
      if (!put.ok) throw new Error("put");
      cleanup();
      navigate("/boardScene");
    } catch {
      setError("Could not start the game.");
    } finally {
      setBusy(false);
    }
  };

  // ---- render -----------------------------------------------------------

  if (phase === "lobby") {
    return (
      <div className="panel-container">
        <h1 className="metal-text">Robot Rally Racer</h1>
        <div className="control-panel">
          <p>
            Game code: <strong>{gameId}</strong>
          </p>
          {role === "host" && (
            <p>
              Password: <strong>{pw}</strong> (share code + password to let others join)
            </p>
          )}
          <h3>Players</h3>
          <ul>
            {players.map((p) => (
              <li key={p.idx}>
                <span style={{ color: ROBOT_COLORS[p.idx % ROBOT_COLORS.length] }}>Robot {p.idx + 1}</span>: {p.name}
              </li>
            ))}
          </ul>
          {role === "host" ? (
            <button className="metal-button" onClick={onStart} disabled={busy || players.length < 1}>
              Start game
            </button>
          ) : (
            <p>Waiting for the host to start...</p>
          )}
          {error && <p className="error-text">{error}</p>}
        </div>
      </div>
    );
  }

  return (
    <div className="panel-container">
      <h1 className="metal-text">Robot Rally Racer</h1>
      <div className="control-panel">
        <label>
          Your name
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={24} />
        </label>
      </div>

      <div className="control-panel">
        <h3>Create a game</h3>
        <label>
          Game name
          <input value={gameName} onChange={(e) => setGameName(e.target.value)} maxLength={40} />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} maxLength={32} />
        </label>
        <label>
          Programming timer
          <select value={timerMs} onChange={(e) => setTimerMs(Number(e.target.value))}>
            {TIMER_OPTIONS.map((o) => (
              <option key={o.ms} value={o.ms}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
        <div className="board-picker-label">Board</div>
        <div className="board-picker">
          {BOARD_CATALOG.map((b) => (
            <button
              type="button"
              key={b.id}
              className={`board-card${selectedBoardId === b.id ? " selected" : ""}`}
              onClick={() => setSelectedBoardId(b.id)}
              aria-pressed={selectedBoardId === b.id}
            >
              <img src={b.preview} alt={b.displayName} className="board-card-preview" />
              <span className="board-card-name">{b.displayName}</span>
              <span className="board-card-meta">
                {b.difficulty} &middot; {b.gameLength} &middot; {b.minPlayers}-{b.maxPlayers}p
              </span>
            </button>
          ))}
        </div>
        <button className="metal-button" onClick={onCreate} disabled={busy}>
          Create
        </button>
      </div>

      <div className="control-panel">
        <h3>Join a game</h3>
        <label>
          Game code
          <input value={joinCode} onChange={(e) => setJoinCode(e.target.value.toUpperCase())} maxLength={8} />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} maxLength={32} />
        </label>
        <button className="metal-button" onClick={onJoin} disabled={busy}>
          Join
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}
    </div>
  );
}
