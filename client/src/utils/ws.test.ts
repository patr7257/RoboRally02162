// Controller specs for client/src/utils/ws.ts (issue #13c/#13d), driven ONLY
// through the public facade (subscribe / sendMessage / closeSocket) plus the
// fetch / EventSource / timer fakes setupTests.ts installs. Nothing here mocks
// ./rrr/* internals directly: every scenario is exercised the way a real
// browser tab would see it, with fetch responses shaped exactly like the real
// /api/robot-rally/* envelopes (independently confirmed live against the
// deployed API by scripts/data-layer-round.mjs), SSE "something changed"
// pings, and the 320ms frame timer emit.ts's animateActivation runs on.
//
// ws.ts / rrr/{transport,store,emit,hostLoop}.ts keep module-level state
// across tests in the same file (they are singletons, imported once), so
// every test seeds fresh identity + fetch responses and tears down with
// closeSocket(0), which resets every slice of that shared state (transport's
// id/version/EventSource/poll timer, the env/busy/animating/lastActivationId
// store, emit.ts's listeners + prompt bookkeeping, and hostLoop's stashes).
import { subscribe, sendMessage, closeSocket } from "./ws";
import type { Envelope } from "./ws";
import { snapshotToGameData } from "./engineAdapter";
import { seedIdentity } from "../testUtils";
import { FakeEventSource } from "../setupTests";
import type {
  GameSnapshot,
  RobotSnapshot,
  Frame,
  ReactionChoice,
  ReactionKind,
} from "../engine/roborally-engine";
import type { HostPrivate } from "./rrr/transport";

// ---- fetch fakery ------------------------------------------------------------

interface FetchResponse {
  status?: number;
  body: unknown;
}

function fakeResponse({ status = 200, body }: FetchResponse) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

let fetchMock: jest.Mock;
/** GET .../view or GET .../state responses, consumed FIFO by fetchState(). */
let stateQueue: FetchResponse[];
/** POST .../intent responses, consumed FIFO by postIntent(). */
let intentQueue: FetchResponse[];

function installFetch(): void {
  stateQueue = [];
  intentQueue = [];
  fetchMock = jest.fn(async (url: string, opts: RequestInit = {}) => {
    const method = (opts.method || "GET").toUpperCase();
    if (url.includes("/intents?")) {
      return fakeResponse({ body: { intents: [], seats: {} } });
    }
    if (method === "POST" && url.includes("/intent")) {
      return fakeResponse(intentQueue.shift() ?? { body: { ok: true } });
    }
    if (url.includes("/view") || url.endsWith("/state")) {
      return fakeResponse(
        stateQueue.shift() ?? { body: { version: 0, unchanged: true, hostBeatAt: null } },
      );
    }
    return fakeResponse({ status: 404, body: { error: "unhandled url: " + url } });
  });
  (globalThis as unknown as { fetch: typeof fetch }).fetch = fetchMock as unknown as typeof fetch;
}

function latestEventSource(): FakeEventSource {
  const instances = FakeEventSource.instances;
  return instances[instances.length - 1];
}

/** Drains the microtask queue so chained `await`s inside the fetch -> reconcile
 *  pipeline (which never touch a real timer outside animateActivation) settle
 *  before assertions run. Fake timers only intercept macrotasks, so this is
 *  independent of jest.advanceTimersByTime. */
async function flush(times = 15): Promise<void> {
  for (let i = 0; i < times; i++) {
    await Promise.resolve();
  }
}

// ---- fixtures ----------------------------------------------------------------

function makeRobot(overrides: Partial<RobotSnapshot> = {}): RobotSnapshot {
  return {
    id: 1,
    x: 0,
    y: 0,
    facing: "N",
    nextCheckpoint: 1,
    alive: true,
    respawnDirection: null,
    ...overrides,
  };
}

function makeSnap(overrides: Partial<GameSnapshot> = {}): GameSnapshot {
  return {
    status: "programming",
    round: 1,
    board: { width: 1, height: 1, tiles: [[{ effects: [] }]] },
    robots: [makeRobot({ id: 1 }), makeRobot({ id: 2 })],
    decks: {
      "1": {
        drawPile: [],
        hand: [
          { action: "MOVE", steps: 1 },
          { action: "SPAM", steps: 0 },
        ],
        discardPile: [],
      },
      "2": { drawPile: [], hand: [], discardPile: [] },
    },
    damageDecks: { spam: 0, trojan: 0, worm: 0 },
    players: [],
    winner: null,
    ...overrides,
  };
}

function makeEnvelope(overrides: Partial<Envelope> & { snap: GameSnapshot }): Envelope {
  return {
    name: "Test Game",
    status: "active",
    phase: "programming",
    round: overrides.snap.round,
    current: 0,
    players: [
      { idx: 0, name: "Host" },
      { idx: 1, name: "Alice" },
    ],
    ...overrides,
  };
}

function frameRobotsFrom(snap: GameSnapshot): Frame["robots"] {
  return snap.robots.map((r) => ({ id: r.id, x: r.x, y: r.y, facing: r.facing, alive: r.alive }));
}

// ---- setup / teardown ---------------------------------------------------------

beforeEach(() => {
  jest.useFakeTimers();
  installFetch();
});

afterEach(() => {
  closeSocket(0);
  jest.useRealTimers();
});

// ---- 1. player reconcile sequence ---------------------------------------------

describe("player reconcile sequence", () => {
  it("turns a programming envelope into stateSnapshot, programmingStarted, hand, discard, damageDecks", async () => {
    seedIdentity({ gameId: "G1", role: "player", seatIdx: 0 });
    const snap = makeSnap({ status: "programming", round: 1 });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap }) } });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();

    expect(messages.map((m) => m.type)).toEqual([
      "stateSnapshot",
      "programmingStarted",
      "hand",
      "discard",
      "damageDecks",
    ]);
    const hand = messages.find((m) => m.type === "hand");
    expect(hand?.payload).toEqual({ hand: ["MOVE1", "SPAM"] });
  });

  it("emits an exact readiness shape (msRemaining fixed, no deadlineAt on the wire) on request", async () => {
    seedIdentity({ gameId: "G1b", role: "player", seatIdx: 0 });
    const snap = makeSnap({ status: "programming", round: 1 });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap }) } });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();
    messages.length = 0;

    sendMessage(JSON.stringify({ payload: { type: "getReadiness" } }));
    expect(messages).toEqual([
      { type: "readiness", payload: { playerSubmitted: { 1: false }, msRemaining: 999000 } },
    ]);
  });
});

// ---- 2. activation animation ---------------------------------------------------

describe("activation animation", () => {
  it("plays roundExecuting, one stateSnapshot + lastMoves per frame, then settles", async () => {
    seedIdentity({ gameId: "G2", role: "player", seatIdx: 0 });
    const programmingSnap = makeSnap({ status: "programming", round: 1 });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap: programmingSnap }) } });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();
    messages.length = 0; // discard the initial programming reconcile

    const settledSnap = makeSnap({ status: "programming", round: 2 });
    const frames: Frame[] = [
      { robots: frameRobotsFrom(settledSnap), label: { robotId: 1, register: 1, text: "MOVE1" } },
      {
        robots: frameRobotsFrom(settledSnap),
        label: { robotId: 2, register: 1, text: "ROTATERIGHT" },
      },
      { robots: frameRobotsFrom(settledSnap), label: { robotId: 1, register: 2, text: "MOVE2" } },
    ];
    stateQueue.push({
      body: {
        version: 2,
        state: makeEnvelope({ snap: settledSnap, activationId: 1, frames }),
      },
    });

    latestEventSource().fire({});
    await flush(); // fetch resolves, reconcile starts the animation: roundExecuting + frame 0

    for (let i = 0; i < frames.length; i++) {
      jest.advanceTimersByTime(320);
      await flush();
    }

    expect(messages.map((m) => m.type)).toEqual([
      "roundExecuting",
      "stateSnapshot",
      "lastMoves",
      "stateSnapshot",
      "lastMoves",
      "stateSnapshot",
      "lastMoves",
      "stateSnapshot", // settle, on the authoritative snapshot
      "programmingStarted",
      "hand",
      "discard",
      "damageDecks",
    ]);

    const settleMessage = messages[7];
    expect(settleMessage.payload).toEqual(snapshotToGameData(settledSnap));

    const lastMovesMessages = messages.filter((m) => m.type === "lastMoves");
    expect(lastMovesMessages[lastMovesMessages.length - 1].payload).toEqual({
      moves: [
        { robotId: 1, move: "MOVE1" },
        { robotId: 2, move: "ROTATERIGHT" },
        { robotId: 1, move: "MOVE2" },
      ],
    });
  });

  it("does not re-animate a repeat rev ping carrying the same activationId", async () => {
    seedIdentity({ gameId: "G3", role: "player", seatIdx: 0 });
    const programmingSnap = makeSnap({ status: "programming", round: 1 });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap: programmingSnap }) } });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();

    const settledSnap = makeSnap({ status: "programming", round: 2 });
    const frames: Frame[] = [{ robots: frameRobotsFrom(settledSnap) }];
    const activatedEnvelope = {
      version: 2,
      state: makeEnvelope({ snap: settledSnap, activationId: 1, frames }),
    };
    stateQueue.push({ body: activatedEnvelope });
    latestEventSource().fire({});
    await flush();
    jest.advanceTimersByTime(320); // the single frame
    await flush();
    jest.advanceTimersByTime(320); // settles
    await flush();

    expect(messages.filter((m) => m.type === "roundExecuting")).toHaveLength(1);

    // Re-fire the SAME envelope (same activationId, same frames).
    messages.length = 0;
    stateQueue.push({ body: activatedEnvelope });
    latestEventSource().fire({});
    await flush();

    expect(messages.filter((m) => m.type === "roundExecuting")).toHaveLength(0);
  });
});

// ---- 4. host restore (#3) -------------------------------------------------------

describe("host restore (#3)", () => {
  it("adopts the published activationId and readiness without replaying frames", async () => {
    seedIdentity({ gameId: "G4", role: "host", seatIdx: 0, hostToken: "host-tok-1" });

    const snap = makeSnap({ status: "programming", round: 3 });
    const frames: Frame[] = [{ robots: frameRobotsFrom(snap) }]; // present, but must not replay
    const hostPrivate: HostPrivate = {
      round: 3,
      hostProgram: ["MOVE1"],
      submittedThisRound: true,
    };
    stateQueue.push({
      body: {
        version: 5,
        state: makeEnvelope({
          snap,
          activationId: 7,
          frames,
          readiness: { 0: true },
          hostPrivate,
        }),
      },
    });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();

    expect(messages.some((m) => m.type === "roundExecuting")).toBe(false);
    expect(messages.map((m) => m.type)).toEqual([
      "stateSnapshot",
      "programmingStarted",
      "hand",
      "discard",
      "damageDecks",
    ]);

    // The restored readiness map (from env.readiness, seeded before the
    // reload) surfaces as an immediate readiness ack, keyed by robotId.
    messages.length = 0;
    sendMessage(JSON.stringify({ payload: { type: "getReadiness" } }));
    expect(messages).toEqual([
      { type: "readiness", payload: { playerSubmitted: { 1: true }, msRemaining: 999000 } },
    ]);
  });
});

// ---- 5. prompt synthesis --------------------------------------------------------

describe("prompt synthesis", () => {
  it("emits reactionNeeded exactly once for my robot, and dedupes a repeat", async () => {
    seedIdentity({ gameId: "G5", role: "player", seatIdx: 0 }); // robotId 1
    const pendingReaction = {
      promptId: "r1-g1-t0-1",
      robotId: 1,
      register: 1,
      kind: "SANDBOX" as ReactionKind,
      options: ["MOVE1", "MOVE2", "MOVE3"] as ReactionChoice[],
      defaultChoice: "MOVE1" as ReactionChoice,
    };
    const snap = makeSnap({ status: "awaiting-reaction", round: 1, pendingReaction });
    const body = { version: 1, state: makeEnvelope({ snap, deadlineAt: 123456 }) };
    stateQueue.push({ body });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();

    expect(messages.filter((m) => m.type === "reactionNeeded")).toEqual([
      {
        type: "reactionNeeded",
        payload: {
          kind: "SANDBOX",
          options: ["MOVE1", "MOVE2", "MOVE3"],
          deadline: 123456,
          promptId: "r1-g1-t0-1",
        },
      },
    ]);

    messages.length = 0;
    stateQueue.push({ body });
    latestEventSource().fire({});
    await flush();
    expect(messages.some((m) => m.type === "reactionNeeded")).toBe(false);
  });

  it("emits needRespawnDirection only for my dead robot, and dedupes a repeat", async () => {
    seedIdentity({ gameId: "G6", role: "player", seatIdx: 0 }); // robotId 1
    const snap = makeSnap({
      status: "awaiting-respawn",
      round: 2,
      robots: [makeRobot({ id: 1, alive: false }), makeRobot({ id: 2, alive: false })],
    });
    const body = { version: 1, state: makeEnvelope({ snap }) };
    stateQueue.push({ body });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();

    expect(messages.filter((m) => m.type === "needRespawnDirection")).toEqual([
      { type: "needRespawnDirection", payload: { robotId: 1 } },
    ]);

    messages.length = 0;
    stateQueue.push({ body });
    latestEventSource().fire({});
    await flush();
    expect(messages.some((m) => m.type === "needRespawnDirection")).toBe(false);
  });
});

// ---- 6. outbound intents --------------------------------------------------------

describe("outbound intents", () => {
  it("submitProgram posts a program intent for the current round; acks on 200 and on 409", async () => {
    seedIdentity({ gameId: "G7", role: "player", seatIdx: 0, playerToken: "ptok-1" });
    const snap = makeSnap({ status: "programming", round: 5 });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap }) } });

    const messages: { type: string; payload?: unknown }[] = [];
    subscribe((m) => messages.push(JSON.parse(m)));
    await flush();
    messages.length = 0;

    intentQueue.push({ status: 200, body: { ok: true } });
    sendMessage(JSON.stringify({ payload: { type: "submitProgram", cards: ["MOVE1", "MOVE2"] } }));
    await flush();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/robot-rally/games/G7/intent",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          playerIdx: 0,
          playerToken: "ptok-1",
          round: 5,
          type: "program",
          registers: ["MOVE1", "MOVE2"],
        }),
      }),
    );
    expect(messages).toEqual([{ type: "ack", payload: { message: "Program submitted" } }]);

    // A 409 (the backend already has a program for this round) is treated as
    // already-submitted: still an ack, never an error.
    messages.length = 0;
    intentQueue.push({ status: 409, body: { error: "already programmed" } });
    sendMessage(JSON.stringify({ payload: { type: "submitProgram", cards: ["MOVE1"] } }));
    await flush();
    expect(messages).toEqual([{ type: "ack", payload: { message: "Program submitted" } }]);
  });

  it("submitReaction posts {type: reaction, promptId, choice}", async () => {
    seedIdentity({ gameId: "G8", role: "player", seatIdx: 0, playerToken: "ptok-2" });
    const pendingReaction = {
      promptId: "r5-g2-t0-1",
      robotId: 1,
      register: 2,
      kind: "WEASEL" as ReactionKind,
      options: ["LEFT", "RIGHT"] as ReactionChoice[],
      defaultChoice: "LEFT" as ReactionChoice,
    };
    const snap = makeSnap({ status: "awaiting-reaction", round: 5, pendingReaction });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap }) } });
    subscribe(() => {});
    await flush();

    intentQueue.push({ status: 200, body: { ok: true } });
    sendMessage(JSON.stringify({ payload: { type: "submitReaction", choice: "LEFT" } }));
    await flush();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/robot-rally/games/G8/intent",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          playerIdx: 0,
          playerToken: "ptok-2",
          round: 5,
          type: "reaction",
          promptId: "r5-g2-t0-1",
          choice: "LEFT",
        }),
      }),
    );
  });

  it("setRespawnDirection posts {type: respawn, direction}", async () => {
    seedIdentity({ gameId: "G9", role: "player", seatIdx: 0, playerToken: "ptok-3" });
    const snap = makeSnap({
      status: "awaiting-respawn",
      round: 6,
      robots: [makeRobot({ id: 1, alive: false })],
    });
    stateQueue.push({ body: { version: 1, state: makeEnvelope({ snap }) } });
    subscribe(() => {});
    await flush();

    intentQueue.push({ status: 200, body: { ok: true } });
    sendMessage(JSON.stringify({ payload: { type: "setRespawnDirection", direction: "N" } }));
    await flush();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/robot-rally/games/G9/intent",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          playerIdx: 0,
          playerToken: "ptok-3",
          round: 6,
          type: "respawn",
          direction: "N",
        }),
      }),
    );
  });
});
