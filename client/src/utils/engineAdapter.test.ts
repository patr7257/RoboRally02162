// Pure-function specs for the engine <-> client bridge. No mocks: every
// function here is exercised with plain object fixtures built from the
// GameSnapshot/CardSnapshot/Frame shapes declared in
// client/src/engine/roborally-engine.d.ts.

import type {
  GameSnapshot,
  CardSnapshot,
  Frame,
  RobotSnapshot,
} from "../engine/roborally-engine";
import type { MoveType } from "../types/boardTypes";
import {
  moveTypeToCard,
  cardToMoveType,
  cardsToMoveTypes,
  moveTypesToCards,
  snapshotToGameData,
  frameToGameData,
  handForRobot,
  discardForRobot,
} from "./engineAdapter";

// ---- fixtures --------------------------------------------------------------

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

function makeSnapshot(overrides: Partial<GameSnapshot> = {}): GameSnapshot {
  return {
    status: "activating",
    round: 1,
    board: {
      width: 3,
      height: 2,
      tiles: [
        [{ effects: [] }, { effects: [{ kind: "CHECKPOINT", number: 1 }] }],
        [{ effects: [] }, { effects: [] }],
        [{ effects: [{ kind: "PITS" }] }, { effects: [] }],
      ],
    },
    robots: [makeRobot()],
    decks: {},
    damageDecks: { spam: 0, trojan: 0, worm: 0 },
    players: [],
    winner: null,
    ...overrides,
  };
}

// ---- card <-> MoveType round trip ------------------------------------------

describe("moveTypeToCard / cardToMoveType round trip", () => {
  const allMoveTypes: MoveType[] = [
    "MOVE1",
    "MOVE2",
    "MOVE3",
    "MOVEBACK",
    "ROTATELEFT",
    "ROTATERIGHT",
    "UTURN",
    "AGAIN",
    "SPEED",
    "SANDBOX",
    "WEASEL",
    "SPAM",
    "TROJAN_HORSE",
    "WORM",
  ];

  it.each(allMoveTypes)("round-trips %s through the engine card shape", (m) => {
    const card = moveTypeToCard(m);
    expect(cardToMoveType(card)).toBe(m);
  });

  it("maps MOVE cards to the correct steps", () => {
    expect(moveTypeToCard("MOVE1")).toEqual({ action: "MOVE", steps: 1 });
    expect(moveTypeToCard("MOVE2")).toEqual({ action: "MOVE", steps: 2 });
    expect(moveTypeToCard("MOVE3")).toEqual({ action: "MOVE", steps: 3 });
    expect(moveTypeToCard("MOVEBACK")).toEqual({ action: "MOVE", steps: -1 });
  });

  it("maps non-MOVE cards to {action: m, steps: 0}", () => {
    expect(moveTypeToCard("SPAM")).toEqual({ action: "SPAM", steps: 0 });
    expect(moveTypeToCard("TROJAN_HORSE")).toEqual({
      action: "TROJAN_HORSE",
      steps: 0,
    });
    expect(moveTypeToCard("WORM")).toEqual({ action: "WORM", steps: 0 });
    expect(moveTypeToCard("SPEED")).toEqual({ action: "SPEED", steps: 0 });
    expect(moveTypeToCard("WEASEL")).toEqual({ action: "WEASEL", steps: 0 });
    expect(moveTypeToCard("SANDBOX")).toEqual({ action: "SANDBOX", steps: 0 });
  });

  it("moveTypesToCards/cardsToMoveTypes round-trip a full hand", () => {
    const hand: MoveType[] = [
      "MOVE1",
      "MOVEBACK",
      "SPAM",
      "TROJAN_HORSE",
      "WORM",
      "SPEED",
      "WEASEL",
      "SANDBOX",
      "ROTATELEFT",
    ];
    const cards: CardSnapshot[] = moveTypesToCards(hand);
    expect(cardsToMoveTypes(cards)).toEqual(hand);
  });
});

// ---- snapshotToGameData -----------------------------------------------------

describe("snapshotToGameData", () => {
  it("carries board dimensions through unchanged", () => {
    const snap = makeSnapshot();
    const data = snapshotToGameData(snap);
    expect(data.board.width).toBe(3);
    expect(data.board.height).toBe(2);
    expect(data.board.tiles).toHaveLength(3);
    expect(data.board.tiles[0]).toHaveLength(2);
  });

  it("assigns x/y coordinates onto each tile matching its column-major position", () => {
    const snap = makeSnapshot();
    const data = snapshotToGameData(snap);
    expect(data.board.tiles[2][0]).toMatchObject({ x: 2, y: 0 });
    expect(data.board.tiles[0][1]).toMatchObject({ x: 0, y: 1 });
  });

  it("maps each robot's fields, carrying nextCheckpoint through unchanged", () => {
    const snap = makeSnapshot({
      robots: [
        makeRobot({ id: 1, x: 2, y: 3, facing: "E", nextCheckpoint: 4 }),
      ],
    });
    const data = snapshotToGameData(snap);
    expect(data.robots).toEqual([
      { id: 1, x: 2, y: 3, facing: "E", nextCheckpoint: 4, alive: true },
    ]);
  });

  it("keeps !alive robots in the render list with alive: false (awaiting-respawn robots stay visible)", () => {
    const snap = makeSnapshot({
      robots: [
        makeRobot({ id: 1, alive: true }),
        makeRobot({ id: 2, alive: false }),
      ],
    });
    const data = snapshotToGameData(snap);
    expect(data.robots.map((r) => r.id)).toEqual([1, 2]);
    expect(data.robots.find((r) => r.id === 2)).toMatchObject({
      id: 2,
      alive: false,
    });
  });
});

// ---- handForRobot / discardForRobot -----------------------------------------

describe("handForRobot / discardForRobot", () => {
  function makeDecks() {
    return {
      "1": {
        drawPile: [],
        hand: moveTypesToCards(["MOVE1", "SPAM"]),
        discardPile: moveTypesToCards(["ROTATELEFT"]),
      },
      "2": {
        drawPile: [],
        hand: moveTypesToCards(["MOVE3", "WORM"]),
        discardPile: moveTypesToCards(["UTURN", "TROJAN_HORSE"]),
      },
    };
  }

  it("selects robot 1's hand and discard, not robot 2's", () => {
    const snap = makeSnapshot({ decks: makeDecks() });
    expect(handForRobot(snap, 1)).toEqual(["MOVE1", "SPAM"]);
    expect(discardForRobot(snap, 1)).toEqual(["ROTATELEFT"]);
  });

  it("selects robot 2's hand and discard, not robot 1's", () => {
    const snap = makeSnapshot({ decks: makeDecks() });
    expect(handForRobot(snap, 2)).toEqual(["MOVE3", "WORM"]);
    expect(discardForRobot(snap, 2)).toEqual(["UTURN", "TROJAN_HORSE"]);
  });

  it("returns an empty array for a robotId with no deck entry", () => {
    const snap = makeSnapshot({ decks: makeDecks() });
    expect(handForRobot(snap, 99)).toEqual([]);
    expect(discardForRobot(snap, 99)).toEqual([]);
  });

  // Extension for issue #8: a full 9-card hand mixing all three damage cards
  // with normal cards, and a discard pile carrying damage cards drawn into it
  // by a reboot penalty / laser hit (engine/src/model/deck.ts addToDiscard),
  // as opposed to a damage card returned to the global pool on play. Both
  // panels the client renders (Hand and Discard Pile) must reflect these
  // without losing or reordering any card.
  it("preserves a full damage-heavy hand's order, including duplicate damage cards", () => {
    const hand: MoveType[] = [
      "MOVE1",
      "SPAM",
      "SPAM",
      "TROJAN_HORSE",
      "WORM",
      "MOVE2",
      "ROTATELEFT",
      "ROTATERIGHT",
      "UTURN",
    ];
    const snap = makeSnapshot({
      decks: { "1": { drawPile: [], hand: moveTypesToCards(hand), discardPile: [] } },
    });
    expect(handForRobot(snap, 1)).toEqual(hand);
  });

  it("renders damage cards a robot took as a hit sitting in the discard pile", () => {
    // Mirrors what Deck.addToDiscard leaves behind after a laser hit or a
    // reboot penalty: damage cards mixed into the discard pile alongside
    // normally-discarded cards, before the next reshuffle/draw picks them up.
    const discard: MoveType[] = ["MOVE1", "SPAM", "SPAM", "WORM"];
    const snap = makeSnapshot({
      decks: { "1": { drawPile: [], hand: [], discardPile: moveTypesToCards(discard) } },
    });
    expect(discardForRobot(snap, 1)).toEqual(discard);
  });
});

// ---- frameToGameData ---------------------------------------------------------

describe("frameToGameData", () => {
  it("overlays frame positions/facing onto the snapshot's board", () => {
    const snap = makeSnapshot({
      robots: [makeRobot({ id: 1, x: 0, y: 0, facing: "N", nextCheckpoint: 2 })],
    });
    const frame: Frame = {
      robots: [{ id: 1, x: 1, y: 1, facing: "S", alive: true }],
    };
    const data = frameToGameData(snap, frame);
    expect(data.board).toEqual(snapshotToGameData(snap).board);
    expect(data.robots).toEqual([
      { id: 1, x: 1, y: 1, facing: "S", nextCheckpoint: 2, alive: true },
    ]);
  });

  it("carries nextCheckpoint from the snapshot, not the frame", () => {
    const snap = makeSnapshot({
      robots: [makeRobot({ id: 1, nextCheckpoint: 5 })],
    });
    const frame: Frame = {
      robots: [{ id: 1, x: 9, y: 9, facing: "W", alive: true }],
    };
    const data = frameToGameData(snap, frame);
    expect(data.robots[0].nextCheckpoint).toBe(5);
  });

  it("defaults nextCheckpoint to 1 for a frame robot absent from the snapshot", () => {
    const snap = makeSnapshot({ robots: [makeRobot({ id: 1 })] });
    const frame: Frame = {
      robots: [{ id: 42, x: 0, y: 0, facing: "N", alive: true }],
    };
    const data = frameToGameData(snap, frame);
    expect(data.robots[0]).toMatchObject({ id: 42, nextCheckpoint: 1 });
  });

  it("keeps !alive robots from the frame with alive: false (awaiting-respawn robots stay visible)", () => {
    const snap = makeSnapshot({
      robots: [makeRobot({ id: 1 }), makeRobot({ id: 2 })],
    });
    const frame: Frame = {
      robots: [
        { id: 1, x: 0, y: 0, facing: "N", alive: true },
        { id: 2, x: 1, y: 1, facing: "E", alive: false },
      ],
    };
    const data = frameToGameData(snap, frame);
    expect(data.robots.map((r) => r.id)).toEqual([1, 2]);
    expect(data.robots.find((r) => r.id === 2)).toMatchObject({
      id: 2,
      alive: false,
    });
  });

  it("carries alive through from the frame robot, not the snapshot", () => {
    const snap = makeSnapshot({
      robots: [makeRobot({ id: 1, alive: true })],
    });
    const frame: Frame = {
      robots: [{ id: 1, x: 0, y: 0, facing: "N", alive: false }],
    };
    const data = frameToGameData(snap, frame);
    expect(data.robots[0]).toMatchObject({ id: 1, alive: false });
  });
});
