import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import type { GameObserver } from "../src/core/gameObserver.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { assertPosDir } from "./util/testSupport.js";
import {
  initBoardWithGreenConveyors,
  initBoardWithBlueConveyors,
  initBoardWithGreenConveyorsWithCheckpoints,
  initBoardWithBlueConveyorsWithCheckpoints,
  initBoardWithGreenConveyorCollision,
  initBoardWithBlueConveyorCollision,
  initBoardWithThreeRobotCollision,
  initBoardWithSecondStepCollision,
  initBoardWithBlueConveyorCancelMove,
  initBoardWithGreenConveyorAndWalls,
  initBoardWithBlueConveyorAndWalls,
  initBoardWithCurvedConveyorAtDestination,
  initBoardWithCurvedConveyorAtDestinationBlue,
  initBoardWithStraightThenCurvedGreenConveyor,
  initBoardWithStraightThenCurvedBlueConveyor,
  initBoardWithStraightBlue,
  initBoardWithStraightGreen,
  initBoardWithCurvedToCurvedGreenConveyor,
  initBoardWithMultipleCurvedBlueConveyors,
} from "./util/boardTestUtils.js";

function winnerCapturingObserver(sink: { value: number | null }): GameObserver {
  return {
    onWinnerDeclared(_g, winner) {
      sink.value = winner;
    },
    onGameUpdate() {},
  };
}

/** Ported from dk.dtu.GameConveyorTest. */
describe("GameConveyor", () => {
  it("greenConveyor moves robot", () => {
    const board = initBoardWithGreenConveyors(5, 5);
    const r = new Robot(1, 1, 0, Direction.E);
    r.loadProgram([]);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r, 2, 4, Direction.S);
  });

  it("blueConveyor moves robot", () => {
    const board = initBoardWithBlueConveyors(10, 10);
    const r = new Robot(1, 1, 0, Direction.E);
    r.loadProgram([]);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r, 7, 4, Direction.E);
  });

  it("blueConveyor moves robot only one space", () => {
    const board = initBoardWithBlueConveyors(10, 10);
    const r = new Robot(1, 7, 4, Direction.E);
    r.loadProgram([]);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r, 8, 4, Direction.E);
  });

  it("greenConveyor moves two robots to checkpoints", () => {
    const board = initBoardWithGreenConveyorsWithCheckpoints(5, 5);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(2, 1, 0, Direction.E);
    r1.loadProgram([]);
    r2.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);
    const observed = { value: null as number | null };
    game.addObserver(winnerCapturingObserver(observed));

    game.startRound();

    assertPosDir(r1, 3, 2, Direction.S);
    assertPosDir(r2, 3, 3, Direction.S);
    expect(r2.getNextCheckpoint()).toBe(2);
    expect(game.getWinner()).toBe(2);
    expect(observed.value).toBe(2);
  });

  it("blueConveyor moves two robots to checkpoints", () => {
    const board = initBoardWithBlueConveyorsWithCheckpoints(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(2, 1, 0, Direction.E);
    r1.loadProgram([]);
    r2.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);
    const observed = { value: null as number | null };
    game.addObserver(winnerCapturingObserver(observed));

    game.startRound();

    assertPosDir(r1, 4, 4, Direction.S);
    assertPosDir(r2, 4, 5, Direction.S);
    expect(r2.getNextCheckpoint()).toBe(2);
    expect(game.getWinner()).toBe(2);
    expect(observed.value).toBe(2);
  });

  it("greenConveyor collision both robots stay in place", () => {
    const board = initBoardWithGreenConveyorCollision(5, 5);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(2, 2, 0, Direction.W);
    r1.loadProgram([]);
    r2.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
    assertPosDir(r2, 2, 0, Direction.W);
  });

  it("blueConveyor collision both robots stay in place", () => {
    const board = initBoardWithBlueConveyorCollision(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(2, 4, 0, Direction.W);
    r1.loadProgram([]);
    r2.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
    assertPosDir(r2, 4, 0, Direction.W);
  });

  it("blueConveyor three robots collision all stay in place", () => {
    const board = initBoardWithThreeRobotCollision(10, 10);
    const r1 = new Robot(1, 0, 1, Direction.E);
    const r2 = new Robot(2, 2, 1, Direction.W);
    const r3 = new Robot(3, 1, 0, Direction.S);
    r1.loadProgram([]);
    r2.loadProgram([]);
    r3.loadProgram([]);
    const robots = [r1, r2, r3];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 1, Direction.E);
    assertPosDir(r2, 2, 1, Direction.W);
    assertPosDir(r3, 1, 0, Direction.S);
  });

  it("blueConveyor collision on second step", () => {
    const board = initBoardWithSecondStepCollision(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(2, 4, 0, Direction.W);
    r1.loadProgram([]);
    r2.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
    assertPosDir(r2, 4, 0, Direction.W);
  });

  it("greenConveyor get cancel move", () => {
    const board = initBoardWithGreenConveyorCollision(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(1, 1, 0, Direction.E);
    r1.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
  });

  it("blueConveyor get cancel move", () => {
    const board = initBoardWithBlueConveyorCancelMove(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(1, 2, 0, Direction.E);
    r1.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
  });

  it("greenConveyorWithWalls get cancel move", () => {
    const board = initBoardWithGreenConveyorAndWalls(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(1, 1, 0, Direction.S);
    r1.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
    assertPosDir(r2, 1, 0, Direction.S);
  });

  it("blueConveyorWithWalls get cancel move", () => {
    const board = initBoardWithBlueConveyorAndWalls(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(1, 1, 0, Direction.S);
    r1.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 0, Direction.E);
    assertPosDir(r2, 1, 0, Direction.S);
  });

  it("robot moves onto curved conveyor by programming does not rotate", () => {
    const board = initBoardWithCurvedConveyorAtDestination(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    r1.loadProgram([ProgramCard.move1()]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 1, 2, Direction.E);
  });

  it("robot moves onto curved blue conveyor by programming does not rotate", () => {
    const board = initBoardWithCurvedConveyorAtDestinationBlue(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    r1.loadProgram([ProgramCard.move1()]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 1, 3, Direction.E);
  });

  it("robot pushed onto curved conveyor does not rotate", () => {
    const board = initBoardWithCurvedConveyorAtDestination(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const r2 = new Robot(2, 1, 0, Direction.E);
    r1.loadProgram([ProgramCard.move1()]);
    r2.loadProgram([]);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 1, 2, Direction.E);
    assertPosDir(r2, 2, 0, Direction.E);
  });

  it("robot moves onto curved blue conveyor by programming", () => {
    const board = initBoardWithBlueConveyors(10, 10);
    const r1 = new Robot(1, 4, 0, Direction.W);
    r1.loadProgram([ProgramCard.move1()]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 8, 4, Direction.S);
  });

  it("robot moves onto curved green conveyor by programming", () => {
    const board = initBoardWithGreenConveyors(10, 10);
    const r1 = new Robot(1, 3, 0, Direction.W);
    r1.loadProgram([ProgramCard.move1()]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 2, 4, Direction.W);
  });

  it("green conveyor from straight to curved", () => {
    const board = initBoardWithStraightThenCurvedGreenConveyor(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    r1.loadProgram([]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 1, 1, Direction.S);
  });

  it("blue conveyor from straight to curved", () => {
    const board = initBoardWithStraightThenCurvedBlueConveyor(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    r1.loadProgram([]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 2, 1, Direction.S);
  });

  it("blue conveyor moves onto conveyor by programming does not rotate", () => {
    const board = initBoardWithStraightBlue(10, 10);
    const r1 = new Robot(1, 0, 1, Direction.N);
    r1.loadProgram([ProgramCard.move1()]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 4, 0, Direction.N);
  });

  it("green conveyor moves onto conveyor by programming does not rotate", () => {
    const board = initBoardWithStraightGreen(10, 10);
    const r1 = new Robot(1, 0, 1, Direction.N);
    r1.loadProgram([ProgramCard.move1()]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 4, 0, Direction.N);
  });

  it("green conveyor from curved to curved does rotate", () => {
    const board = initBoardWithCurvedToCurvedGreenConveyor(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    r1.loadProgram([]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 1, Direction.W);
  });

  it("blue conveyor curved to curved rotates correctly", () => {
    const board = initBoardWithMultipleCurvedBlueConveyors(10, 10);
    const r1 = new Robot(1, 0, 0, Direction.E);
    r1.loadProgram([]);
    const robots = [r1];
    const api = new BoardApiImpl(board, robots);
    new Game(board, api, robots).startRound();
    assertPosDir(r1, 0, 1, Direction.W);
  });
});
