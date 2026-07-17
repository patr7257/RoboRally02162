import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import type { GameObserver } from "../src/core/gameObserver.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import {
  initBoardWithCheckPoints,
  initBoardWithCheckPointsInDifferentNumber,
  initBoardWithCheckPointsInThreeDifferentNumber,
} from "./util/boardTestUtils.js";

function winnerCapturingObserver(sink: { value: number | null }): GameObserver {
  return {
    onWinnerDeclared(_g, winner) {
      sink.value = winner;
    },
    onGameUpdate() {},
  };
}

/** Ported from dk.dtu.GameCheckpointTest. */
describe("GameCheckpoint", () => {
  it("robot wins after completing checkpoints in order", () => {
    const board = initBoardWithCheckPoints(3, 3);
    const r = new Robot(1, 0, 1, Direction.E);
    r.loadProgram([ProgramCard.move1()]);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    const observed = { value: null as number | null };
    game.addObserver(winnerCapturingObserver(observed));

    r.loadProgram([ProgramCard.move1()]);
    game.startRound();

    expect(r.getNextCheckpoint()).toBe(2);
    expect(game.getWinner()).toBeNull();

    r.loadProgram([
      ProgramCard.right(),
      ProgramCard.move1(),
      ProgramCard.left(),
      ProgramCard.move1(),
    ]);
    game.startRound();
    expect(r.getX()).toBe(2);
    expect(r.getY()).toBe(2);
    expect(r.getNextCheckpoint()).toBe(3);
    expect(game.getWinner()).toBe(1);
    expect(observed.value).toBe(1);
  });

  it("robot does not win completing checkpoints in wrong order", () => {
    const board = initBoardWithCheckPointsInDifferentNumber(3, 3);
    const r = new Robot(1, 0, 1, Direction.E);
    r.loadProgram([ProgramCard.move1()]);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    const observed = { value: null as number | null };
    game.addObserver(winnerCapturingObserver(observed));

    r.loadProgram([ProgramCard.move1()]);
    game.startRound();

    expect(r.getNextCheckpoint()).toBe(1);
    expect(game.getWinner()).toBeNull();

    r.loadProgram([
      ProgramCard.right(),
      ProgramCard.move1(),
      ProgramCard.left(),
      ProgramCard.move1(),
    ]);
    game.startRound();
    expect(r.getX()).toBe(2);
    expect(r.getY()).toBe(2);
    expect(r.getNextCheckpoint()).toBe(2);
    expect(game.getWinner()).toBeNull();
    expect(observed.value).toBeNull();
  });

  it("robots do not win when second robot skips checkpoints", () => {
    const board = initBoardWithCheckPointsInThreeDifferentNumber(3, 3);
    const r1 = new Robot(1, 0, 1, Direction.E);
    const r2 = new Robot(2, 1, 2, Direction.E);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    const observed = { value: null as number | null };
    game.addObserver(winnerCapturingObserver(observed));

    r1.loadProgram([ProgramCard.move1()]);
    r2.loadProgram([ProgramCard.move1()]);
    game.startRound();

    expect(r1.getX()).toBe(1);
    expect(r1.getY()).toBe(1);
    expect(r1.getNextCheckpoint()).toBe(2);
    expect(r2.getNextCheckpoint()).toBe(1);
    expect(game.getWinner()).toBeNull();
    expect(observed.value).toBeNull();

    r1.loadProgram([ProgramCard.right(), ProgramCard.move1()]);
    r2.loadProgram([]);
    game.startRound();

    expect(r1.getX()).toBe(1);
    expect(r1.getY()).toBe(2);
    expect(r2.getX()).toBe(2);
    expect(r2.getY()).toBe(2);
    expect(r1.getNextCheckpoint()).toBe(3);
    expect(r2.getNextCheckpoint()).toBe(1);
    expect(game.getWinner()).toBeNull();
    expect(observed.value).toBeNull();
  });
});
