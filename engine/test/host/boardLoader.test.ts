import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { boardFromSnapshot, cardToSnapshot } from "../../src/host/snapshot.js";
import { parseBoardDefinition } from "../../src/host/boardLoader.js";
import {
  createGame,
  submitProgram,
  runActivation,
} from "../../src/host/hostGame.js";

function loadStarterCourse() {
  const path = fileURLToPath(
    new URL("../../../client/public/board.json", import.meta.url),
  );
  const def = JSON.parse(readFileSync(path, "utf8"));
  return parseBoardDefinition(def);
}

const prog = (...cards: ProgramCard[]) => cards.map(cardToSnapshot);

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

    // Robot 1 turns around (W -> E) and drives two tiles east onto the board.
    snap = submitProgram(
      snap,
      1,
      prog(ProgramCard.uturn(), ProgramCard.move1(), ProgramCard.move1()),
    );
    // Robot 2 holds position this round.
    snap = submitProgram(snap, 2, prog());

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
