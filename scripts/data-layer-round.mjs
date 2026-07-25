#!/usr/bin/env node
// Data-layer round trip against the REAL RoboRally Vercel backend
// (/api/robot-rally/*, implemented in the sibling patrickrobelweb repo at
// website/src/app/api/robot-rally/** and website/src/lib/roborally-redis.ts).
// No mocks: this drives create -> seat -> program -> read -> write ->
// resume -> heartbeat against a live server and asserts the exact shapes
// client/src/utils/rrr/transport.ts relies on.
//
// Run (Node 20+, no dependencies, plain fetch):
//   node scripts/data-layer-round.mjs
//
// The server must already be running at BASE_URL (env var, default
// http://localhost:3210). To bring it up locally:
//   cd ../patrickrobelweb/website; pnpm sync:roborally; pnpm build; $env:PORT=3210; pnpm start
//
// The game name below deliberately contains "e2e": the backend's isE2eGame()
// check (lib/roborally-redis.ts) gives such games a 1 hour TTL and keeps them
// out of the public games index, so this script never litters real state.
//
// Sequential checks, each logged PASS/FAIL. Exits 1 on the first failure
// (later checks depend on earlier ones, e.g. tokens/versions), 0 once every
// check has passed.

const BASE_URL = process.env.BASE_URL || "http://localhost:3210";
const API = `${BASE_URL}/api/robot-rally`;

let passCount = 0;
let failCount = 0;

function log(ok, name, detail) {
  const label = ok ? "PASS" : "FAIL";
  console.log(`[${label}] ${name}${detail ? " - " + detail : ""}`);
  if (ok) passCount++;
  else failCount++;
}

/** Logs the check; on failure, prints a summary and exits the process. */
function assertOrExit(name, ok, detail) {
  log(ok, name, detail);
  if (!ok) {
    console.error(`\nAborted after ${passCount} passed, ${failCount} failed.`);
    process.exit(1);
  }
}

function shapesEqual(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected);
}

async function req(method, path, body, headers) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: { "Content-Type": "application/json", ...(headers || {}) },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  let data = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  return { status: res.status, ok: res.ok, data };
}

async function main() {
  console.log(`Data-layer round trip against ${API}\n`);

  // Quick reachability probe before committing to the full sequence.
  let reachable = false;
  try {
    const ping = await fetch(`${API}/games`);
    reachable = ping.ok || ping.status === 503;
  } catch {
    reachable = false;
  }
  if (!reachable) {
    console.error(
      `Cannot reach ${API}/games - is the server running at ${BASE_URL}?\n` +
        "Start it with: cd ../patrickrobelweb/website; pnpm sync:roborally; pnpm build; " +
        '$env:PORT=3210; pnpm start',
    );
    process.exit(1);
  }

  const gameName = "e2e-datalayer-run";
  const password = "e2e-pass-1234";

  // ---- 1. create game ---------------------------------------------------
  const createBody = {
    name: gameName,
    password,
    state: {
      gameId: "",
      name: gameName,
      version: 1,
      status: "lobby",
      createdAt: 0,
      updatedAt: 0,
      round: 0,
      current: 0,
      phase: "lobby",
      players: [{ idx: 0, name: "Host" }],
    },
  };
  const create = await req("POST", "/games", createBody);
  assertOrExit(
    "1. POST /games creates a game and mints gameId + hostToken",
    create.ok && !!create.data?.gameId && !!create.data?.hostToken,
    JSON.stringify(create.data),
  );

  const gameId = create.data.gameId;
  let hostToken = create.data.hostToken;
  let version = create.data.version;
  console.log(`  gameId=${gameId}`);

  // ---- 2. seats -----------------------------------------------------------
  const deviceA = "device-alice-0001";
  const seatA1 = await req("POST", `/games/${gameId}/seats`, {
    pw: password,
    playerIdx: 1,
    deviceId: deviceA,
    name: "Alice",
  });
  assertOrExit(
    "2a. POST seats claims playerIdx 1 -> playerToken",
    seatA1.ok && !!seatA1.data?.playerToken,
    JSON.stringify(seatA1.data),
  );
  const playerToken = seatA1.data.playerToken;

  const seatA2 = await req("POST", `/games/${gameId}/seats`, {
    pw: password,
    playerIdx: 1,
    deviceId: deviceA,
    name: "Alice",
  });
  assertOrExit(
    "2b. Same device re-claiming seat 1 is idempotent (same token)",
    seatA2.ok && seatA2.data?.playerToken === playerToken,
    JSON.stringify(seatA2.data),
  );

  const deviceB = "device-bob-00002";
  const seatB = await req("POST", `/games/${gameId}/seats`, {
    pw: password,
    playerIdx: 1,
    deviceId: deviceB,
    name: "Bob",
  });
  assertOrExit(
    "2c. Different device claiming the same seat -> 409 taken",
    seatB.status === 409,
    JSON.stringify(seatB.data),
  );

  // ---- 3. intent: program ---------------------------------------------------
  const registers = ["MOVE1", "MOVE1", "MOVE1", "ROTATERIGHT", "MOVE2"];
  const intent1 = await req("POST", `/games/${gameId}/intent`, {
    playerIdx: 1,
    playerToken,
    round: 0,
    type: "program",
    registers,
  });
  assertOrExit(
    "3a. POST intent (program) -> 200",
    intent1.ok,
    JSON.stringify(intent1.data),
  );

  const intent2 = await req("POST", `/games/${gameId}/intent`, {
    playerIdx: 1,
    playerToken,
    round: 0,
    type: "program",
    registers,
  });
  assertOrExit(
    "3b. Repeating the same round's program -> 409 already programmed",
    intent2.status === 409,
    JSON.stringify(intent2.data),
  );

  // ---- 4. GET intents (host) --------------------------------------------
  const intents0 = await req("GET", `/games/${gameId}/intents?round=0`, undefined, {
    "x-rrr-host-token": hostToken,
  });
  const hasProgramIntent =
    intents0.ok &&
    Array.isArray(intents0.data?.intents) &&
    intents0.data.intents.some(
      (it) =>
        it.type === "program" &&
        it.playerIdx === 1 &&
        shapesEqual(it.registers, registers),
    );
  assertOrExit(
    "4a. GET intents?round=0 (host) contains the program intent",
    hasProgramIntent,
    JSON.stringify(intents0.data),
  );
  const seatsRedacted =
    intents0.data?.seats &&
    Object.values(intents0.data.seats).every((s) => !("playerToken" in s));
  assertOrExit(
    "4b. GET intents seats are redacted (no playerToken field)",
    !!seatsRedacted,
    JSON.stringify(intents0.data?.seats),
  );

  // ---- 5. PUT state: optimistic concurrency ------------------------------
  function envelope(overrides = {}) {
    return {
      gameId,
      name: gameName,
      version: 1, // ignored by the server; it stamps baseVersion + 1
      status: "active",
      createdAt: 0,
      updatedAt: 0,
      round: 0,
      current: 0,
      phase: "lobby",
      players: [
        { idx: 0, name: "Host" },
        { idx: 1, name: "Alice" },
      ],
      ...overrides,
    };
  }

  const stalePut = await req("PUT", `/games/${gameId}/state`, {
    baseVersion: version - 1,
    state: envelope(),
  }, { "x-rrr-host-token": hostToken });
  assertOrExit(
    "5a. PUT state with a stale baseVersion -> 409 {currentVersion}",
    stalePut.status === 409 && stalePut.data?.currentVersion === version,
    JSON.stringify(stalePut.data),
  );

  const activePut = await req("PUT", `/games/${gameId}/state`, {
    baseVersion: version,
    state: envelope({ status: "active" }),
  }, { "x-rrr-host-token": hostToken });
  assertOrExit(
    "5b. PUT state with the correct baseVersion increments version",
    activePut.ok && activePut.data?.version === version + 1,
    JSON.stringify(activePut.data),
  );
  version = activePut.data.version;

  // ---- 6. GET view: unchanged + hostBeatAt -------------------------------
  const unchangedView = await req("GET", `/games/${gameId}/view?pw=${encodeURIComponent(password)}&v=${version}`);
  assertOrExit(
    "6a. GET view?v=<current> -> {unchanged: true}",
    unchangedView.ok && unchangedView.data?.unchanged === true,
    JSON.stringify(unchangedView.data),
  );
  assertOrExit(
    "6b. GET view response includes a hostBeatAt key (null or number)",
    unchangedView.data && "hostBeatAt" in unchangedView.data,
    JSON.stringify(unchangedView.data),
  );

  // ---- 7. hostPrivate redaction ------------------------------------------
  const hostPrivate = { round: 1, hostProgram: ["MOVE1"], submittedThisRound: true };
  const hostPrivatePut = await req("PUT", `/games/${gameId}/state`, {
    baseVersion: version,
    state: envelope({ status: "active", phase: "programming", round: 1, hostPrivate }),
  }, { "x-rrr-host-token": hostToken });
  assertOrExit(
    "7a. PUT state carrying hostPrivate succeeds",
    hostPrivatePut.ok,
    JSON.stringify(hostPrivatePut.data),
  );
  version = hostPrivatePut.data.version;

  const viewAfterHostPrivate = await req(
    "GET",
    `/games/${gameId}/view?pw=${encodeURIComponent(password)}&v=0`,
  );
  assertOrExit(
    "7b. GET view (player) does NOT include hostPrivate",
    viewAfterHostPrivate.ok && !("hostPrivate" in (viewAfterHostPrivate.data?.state || {})),
    JSON.stringify(viewAfterHostPrivate.data?.state),
  );

  const stateAfterHostPrivate = await req("GET", `/games/${gameId}/state`, undefined, {
    "x-rrr-host-token": hostToken,
  });
  assertOrExit(
    "7c. GET state (host) DOES include hostPrivate",
    stateAfterHostPrivate.ok &&
      shapesEqual(stateAfterHostPrivate.data?.state?.hostPrivate, hostPrivate),
    JSON.stringify(stateAfterHostPrivate.data?.state?.hostPrivate),
  );

  // ---- 8. seats guard once the game has left the lobby -------------------
  const deviceC = "device-carol-0003";
  const seatNewIdx = await req("POST", `/games/${gameId}/seats`, {
    pw: password,
    playerIdx: 2,
    deviceId: deviceC,
    name: "Carol",
  });
  assertOrExit(
    '8a. Claiming a NEW seat on an active game -> 409 "already started"',
    seatNewIdx.status === 409 && seatNewIdx.data?.error === "already started",
    JSON.stringify(seatNewIdx.data),
  );

  const seatReclaim = await req("POST", `/games/${gameId}/seats`, {
    pw: password,
    playerIdx: 1,
    deviceId: deviceA,
    name: "Alice",
  });
  assertOrExit(
    "8b. Same-device re-claim of seat 1 still returns the token",
    seatReclaim.ok && seatReclaim.data?.playerToken === playerToken,
    JSON.stringify(seatReclaim.data),
  );

  // ---- 9. resume ----------------------------------------------------------
  const resumeWrongPw = await req("POST", `/games/${gameId}/resume`, {
    password: "not-the-password",
  });
  assertOrExit(
    "9a. POST resume with the wrong password -> 403",
    resumeWrongPw.status === 403,
    JSON.stringify(resumeWrongPw.data),
  );

  const resumeOk = await req("POST", `/games/${gameId}/resume`, {
    password,
    force: true,
  });
  assertOrExit(
    "9b. POST resume with the right password + force:true -> new hostToken",
    resumeOk.ok && !!resumeOk.data?.hostToken && resumeOk.data.hostToken !== hostToken,
    JSON.stringify(resumeOk.data),
  );
  const oldHostToken = hostToken;
  hostToken = resumeOk.data.hostToken;

  const staleTokenGet = await req("GET", `/games/${gameId}/state`, undefined, {
    "x-rrr-host-token": oldHostToken,
  });
  assertOrExit(
    "9c. The OLD hostToken is now unauthorized (401) on GET state",
    staleTokenGet.status === 401,
    JSON.stringify(staleTokenGet.data),
  );

  const newTokenGet = await req("GET", `/games/${gameId}/state`, undefined, {
    "x-rrr-host-token": hostToken,
  });
  assertOrExit(
    "9d. The NEW hostToken works on GET state",
    newTokenGet.ok,
    JSON.stringify(newTokenGet.data),
  );

  // ---- 10. heartbeat --------------------------------------------------------
  const beat = await req("POST", `/games/${gameId}/heartbeat`, undefined, {
    "x-rrr-host-token": hostToken,
  });
  assertOrExit(
    "10a. POST heartbeat with the new token -> {ok: true}",
    beat.ok && beat.data?.ok === true,
    JSON.stringify(beat.data),
  );

  const viewAfterBeat = await req(
    "GET",
    `/games/${gameId}/view?pw=${encodeURIComponent(password)}&v=0`,
  );
  const beatAt = viewAfterBeat.data?.hostBeatAt;
  const recent = typeof beatAt === "number" && Date.now() - beatAt < 10_000;
  assertOrExit(
    "10b. GET view reports a recent hostBeatAt after the heartbeat",
    recent,
    `hostBeatAt=${beatAt}`,
  );

  // ---- EXTENSION (issue #8): damage-card program intents ------------------
  // SPAM / TROJAN_HORSE / WORM are register values the engine puts into a
  // robot's program (see engine/src/model/damageDecks.ts and
  // engine/src/program/programCard.ts's Action union). This confirms the
  // backend's intent endpoint accepts them as ordinary register strings,
  // including staying under the 40-char id limit the backend enforces on
  // register values. Unlike checks 1-23 above (which the whole script
  // already requires the server for, via the reachability probe at the very
  // top), this section re-checks reachability itself and degrades to a
  // SKIPPED log instead of aborting: it exercises no state the rest of the
  // script depends on, so it is safe to treat as optional. This does not
  // change the pass/fail semantics of the 23 checks above.
  let extensionReachable = false;
  try {
    const ping = await fetch(`${API}/games`);
    extensionReachable = ping.ok || ping.status === 503;
  } catch {
    extensionReachable = false;
  }

  if (!extensionReachable) {
    console.log(
      "[SKIPPED] 11. Damage-card program intents (SPAM/TROJAN_HORSE/WORM) - server not reachable",
    );
  } else {
    const damageRegisters = ["SPAM", "TROJAN_HORSE", "WORM", "MOVE1", "MOVE1"];

    for (const id of damageRegisters) {
      assertOrExit(
        `11a. register id "${id}" fits the 40-char limit`,
        id.length <= 40,
        `length=${id.length}`,
      );
    }

    const damageIntent = await req("POST", `/games/${gameId}/intent`, {
      playerIdx: 1,
      playerToken,
      round: 1,
      type: "program",
      registers: damageRegisters,
    });
    assertOrExit(
      "11b. POST intent (program) with SPAM/TROJAN_HORSE/WORM registers -> 200",
      damageIntent.ok,
      JSON.stringify(damageIntent.data),
    );

    const intents1 = await req("GET", `/games/${gameId}/intents?round=1`, undefined, {
      "x-rrr-host-token": hostToken,
    });
    const hasDamageIntent =
      intents1.ok &&
      Array.isArray(intents1.data?.intents) &&
      intents1.data.intents.some(
        (it) =>
          it.type === "program" &&
          it.playerIdx === 1 &&
          shapesEqual(it.registers, damageRegisters),
      );
    assertOrExit(
      "11c. GET intents?round=1 (host) reflects the damage-card registers unchanged",
      hasDamageIntent,
      JSON.stringify(intents1.data),
    );
  }

  console.log(`\nAll ${passCount} checks passed.`);
}

main().catch((err) => {
  console.error("\nUnexpected error:", err);
  process.exit(1);
});
