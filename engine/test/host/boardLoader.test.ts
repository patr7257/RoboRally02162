import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { boardFromSnapshot } from "../../src/host/snapshot.js";
import { parseBoardDefinition } from "../../src/host/boardLoader.js";
import {
  createGame,
  submitProgram,
  runActivation,
} from "../../src/host/hostGame.js";
import { prog, withHand, withProgram } from "../util/hostTestUtils.js";

function loadStarterCourse() {
  const path = fileURLToPath(
    new URL("../../../client/public/board.json", import.meta.url),
  );
  const def = JSON.parse(readFileSync(path, "utf8"));
  return parseBoardDefinition(def);
}

function loadFixture(name: string) {
  const path = fileURLToPath(
    new URL(`../fixtures/boards/${name}.json`, import.meta.url),
  );
  const def = JSON.parse(readFileSync(path, "utf8"));
  return { def, loaded: parseBoardDefinition(def) };
}

/** Every "x,y" key with an array value in the raw definition, decoded. */
function effectCoordinates(def: Record<string, unknown>): Array<{
  x: number;
  y: number;
  count: number;
}> {
  const effects = (def.effects ?? {}) as Record<string, unknown>;
  const coords: Array<{ x: number; y: number; count: number }> = [];
  for (const key of Object.keys(effects)) {
    const value = effects[key];
    if (!Array.isArray(value)) continue;
    const [x, y] = key.split(",").map(Number);
    coords.push({ x, y, count: value.length });
  }
  return coords;
}

describe("boardLoader on the real Starter-Course", () => {
  it("parses dimensions, starting tiles and effects", () => {
    const loaded = loadStarterCourse();

    expect(loaded.displayName).toBe("Starter-Course");
    // startingBoardWidth 3 + boardWidth 10 = 13 wide, boardHeight 10.
    expect(loaded.board.width).toBe(13);
    expect(loaded.board.height).toBe(10);
    expect(loaded.startDirection).toBe(Direction.W);

    // Six starting tiles, keyed by 1-based playerId.
    expect(Object.keys(loaded.startingTiles)).toHaveLength(6);
    expect(loaded.startingTiles[1]).toEqual({ x: 1, y: 1 });

    // Checkpoint 1 sits at (12,3).
    const cp = loaded.board.tiles[12][3].effects;
    expect(cp).toContainEqual({ kind: "CHECKPOINT", number: 1 });

    // A board laser with its wall at (6,3).
    const laserTile = loaded.board.tiles[6][3].effects.map((e) => e.kind);
    expect(laserTile).toContain("WALL");
    expect(laserTile).toContain("BOARD_LASER");
  });

  it("builds an engine board without error", () => {
    const loaded = loadStarterCourse();
    const board = boardFromSnapshot(loaded.board);
    expect(board.getWidth()).toBe(13);
    expect(board.getHeight()).toBe(10);
    // Effects are live engine objects on the checkpoint tile.
    expect(board.getTile(12, 3).getEffects().length).toBeGreaterThan(0);
  });

  it("runs a full round for two robots on the real board", () => {
    const loaded = loadStarterCourse();
    const s1 = loaded.startingTiles[1];
    const s2 = loaded.startingTiles[2];

    let snap = createGame(loaded.board, [
      {
        robotId: 1,
        name: "Ada",
        color: "#f00",
        x: s1.x,
        y: s1.y,
        facing: loaded.startDirection,
      },
      {
        robotId: 2,
        name: "Bo",
        color: "#0f0",
        x: s2.x,
        y: s2.y,
        facing: loaded.startDirection,
      },
    ]);

    // Robot 1 turns around (W -> E), drives two tiles east onto the board and
    // then spends the last two registers turning back to east.
    const picked = [
      ProgramCard.uturn(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.left(),
      ProgramCard.right(),
    ];
    snap = withHand(snap, 1, picked);
    snap = submitProgram(snap, 1, prog(...picked));
    // Robot 2 holds position this round.
    snap = withProgram(snap, 2, []);

    const { snapshot: next, frames } = runActivation(snap);

    const r1 = next.robots.find((r) => r.id === 1)!;
    expect([r1.x, r1.y]).toEqual([3, 1]);
    expect(r1.facing).toBe(Direction.E);
    expect(r1.alive).toBe(true);

    const r2 = next.robots.find((r) => r.id === 2)!;
    expect(r2.alive).toBe(true);

    expect(frames.length).toBeGreaterThan(1);
    expect(next.round).toBe(2);
    expect(next.winner).toBeNull();
  });
});

describe("boardLoader orientation handling", () => {
  it("burnout (n): horizontal starting band on top, nothing dropped", () => {
    const { def, loaded } = loadFixture("burnout");

    // boardWidth 10, boardHeight 10 + startingBoardHeight 3 = 13 tall.
    expect(loaded.board.width).toBe(10);
    expect(loaded.board.height).toBe(13);
    expect(loaded.startDirection).toBe(Direction.N);

    // Antenna sits in the starting band at (6,0).
    const antennaKinds = loaded.board.tiles[6][0].effects.map((e) => e.kind);
    expect(antennaKinds).toContain("ANTENNA");

    // Six starting tiles, all inside the starting strip (y < startingBoardHeight).
    const startingCoords = Object.values(loaded.startingTiles);
    expect(startingCoords).toHaveLength(6);
    for (const { y } of startingCoords) {
      expect(y).toBeLessThan(def.startingBoardHeight);
    }

    // Every array-valued effect key in the fixture must land on the parsed
    // board: nothing gets silently dropped by the bounds guard.
    const fixtureCoords = effectCoordinates(def);
    const expectedTotal = fixtureCoords.reduce((sum, c) => sum + c.count, 0);
    let placedTotal = 0;
    for (const { x, y, count } of fixtureCoords) {
      const placed = loaded.board.tiles[x][y].effects.length;
      expect(placed).toBe(count);
      placedTotal += placed;
    }
    expect(placedTotal).toBe(expectedTotal);
  });

  it("fractionation (s): horizontal starting band on bottom, nothing dropped", () => {
    const { def, loaded } = loadFixture("fractionation");

    // boardWidth 10, boardHeight 10 + startingBoardHeight 3 = 13 tall.
    expect(loaded.board.width).toBe(10);
    expect(loaded.board.height).toBe(13);
    expect(loaded.startDirection).toBe(Direction.S);

    // Antenna sits in the starting band at (4,12).
    const antennaKinds = loaded.board.tiles[4][12].effects.map((e) => e.kind);
    expect(antennaKinds).toContain("ANTENNA");

    // Six starting tiles, all inside the starting strip at the bottom
    // (y >= boardHeight).
    const startingCoords = Object.values(loaded.startingTiles);
    expect(startingCoords).toHaveLength(6);
    for (const { y } of startingCoords) {
      expect(y).toBeGreaterThanOrEqual(def.boardHeight);
    }

    // Every array-valued effect key in the fixture must land on the parsed
    // board: nothing gets silently dropped by the bounds guard.
    const fixtureCoords = effectCoordinates(def);
    const expectedTotal = fixtureCoords.reduce((sum, c) => sum + c.count, 0);
    let placedTotal = 0;
    for (const { x, y, count } of fixtureCoords) {
      const placed = loaded.board.tiles[x][y].effects.length;
      expect(placed).toBe(count);
      placedTotal += placed;
    }
    expect(placedTotal).toBe(expectedTotal);
  });

  it("death-trap (e): vertical starting band on the right, dims unchanged", () => {
    const { def, loaded } = loadFixture("death-trap");

    // startingBoardWidth 3 + boardWidth 10 = 13 wide, boardHeight 10 tall.
    expect(loaded.board.width).toBe(13);
    expect(loaded.board.height).toBe(10);
    expect(loaded.startDirection).toBe(Direction.E);

    const fixtureCoords = effectCoordinates(def);
    const expectedTotal = fixtureCoords.reduce((sum, c) => sum + c.count, 0);
    let placedTotal = 0;
    for (const { x, y, count } of fixtureCoords) {
      const placed = loaded.board.tiles[x][y].effects.length;
      expect(placed).toBe(count);
      placedTotal += placed;
    }
    expect(placedTotal).toBe(expectedTotal);
  });

  it("Starter-Course (w): vertical starting band on the left, unchanged behavior", () => {
    const { def, loaded } = loadFixture("Starter-Course");

    // startingBoardWidth 3 + boardWidth 10 = 13 wide, boardHeight 10 tall.
    expect(loaded.board.width).toBe(13);
    expect(loaded.board.height).toBe(10);
    expect(loaded.startDirection).toBe(Direction.W);
    expect(loaded.startingTiles[1]).toEqual({ x: 1, y: 1 });

    const fixtureCoords = effectCoordinates(def);
    const expectedTotal = fixtureCoords.reduce((sum, c) => sum + c.count, 0);
    let placedTotal = 0;
    for (const { x, y, count } of fixtureCoords) {
      const placed = loaded.board.tiles[x][y].effects.length;
      expect(placed).toBe(count);
      placedTotal += placed;
    }
    expect(placedTotal).toBe(expectedTotal);
  });
});
