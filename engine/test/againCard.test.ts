import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { initBoardWithCheckPoints } from "./util/boardTestUtils.js";

/** Ported from dk.dtu.AgainCardTest. */
describe("AgainCard", () => {
  it("again card repeats movement from previous register", () => {
    const board = initBoardWithCheckPoints(5, 5);
    const r = new Robot(1, 0, 0, Direction.E);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([ProgramCard.move2(), ProgramCard.again()]);
    game.startRound();

    expect(r.getX()).toBe(4);
    expect(r.getY()).toBe(0);
    expect(r.getDirection()).toBe(Direction.E);
  });

  it("again card repeats rotation from previous register", () => {
    const board = initBoardWithCheckPoints(5, 5);
    const r = new Robot(1, 2, 2, Direction.N);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([ProgramCard.right(), ProgramCard.again(), ProgramCard.move1()]);
    game.startRound();

    expect(r.getX()).toBe(2);
    expect(r.getY()).toBe(3);
    expect(r.getDirection()).toBe(Direction.S);
  });

  it("again card does nothing when used in first register", () => {
    const board = initBoardWithCheckPoints(6, 6);
    const r = new Robot(1, 1, 1, Direction.E);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([
      ProgramCard.again(),
      ProgramCard.move2(),
      ProgramCard.again(),
    ]);
    game.startRound();

    expect(r.getX()).toBe(5);
    expect(r.getY()).toBe(1);
    expect(r.getDirection()).toBe(Direction.E);
  });

  it("consecutive again cards only repeat original action", () => {
    const board = initBoardWithCheckPoints(5, 5);
    const r = new Robot(1, 0, 0, Direction.S);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([
      ProgramCard.move1(),
      ProgramCard.again(),
      ProgramCard.again(),
    ]);
    game.startRound();

    expect(r.getX()).toBe(0);
    expect(r.getY()).toBe(3);
    expect(r.getDirection()).toBe(Direction.S);
  });

  it("again card works with backup movement", () => {
    const board = initBoardWithCheckPoints(5, 5);
    const r = new Robot(1, 2, 2, Direction.E);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([ProgramCard.back1(), ProgramCard.again()]);
    game.startRound();

    expect(r.getX()).toBe(0);
    expect(r.getY()).toBe(2);
    expect(r.getDirection()).toBe(Direction.E);
  });
});
