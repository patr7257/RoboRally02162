import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { Action, ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { Coord, Edge } from "../src/rules/coord.js";
import { DestroyCause } from "../src/rules/outcome.js";
import { initEmptyBoard } from "./util/boardTestUtils.js";
import {
  assertPosDir,
  assertMoved,
  assertMove,
  assertDestroy,
  assertEdgeBlock,
  assertChainBlockedByEdge,
  lineRobots,
  walls,
} from "./util/testSupport.js";

/** Ported from dk.dtu.GameSimulationTest. */
describe("GameSimulation", () => {
  it.each([
    [1, 2],
    [2, 3],
    [3, 4],
    [-1, 0],
  ])("program MOVE %i from (1,1,E) -> expected x=%i", (steps, expectedX) => {
    const b = initEmptyBoard(5, 5);
    const r = new Robot(1, 1, 1, Direction.E);
    r.loadProgram([new ProgramCard(Action.MOVE, steps)]);
    const api = new BoardApiImpl(b, [r]);

    new Game(b, api, [r]).startRound();

    assertPosDir(r, expectedX, 1, Direction.E);
  });

  it.each([
    [Action.ROTATERIGHT, Direction.E, Direction.S],
    [Action.ROTATELEFT, Direction.E, Direction.N],
    [Action.UTURN, Direction.E, Direction.W],
  ])("program ROTATE %s from %s -> %s", (action, start, expected) => {
    const b = initEmptyBoard(3, 3);
    const r = new Robot(1, 1, 1, start);
    r.loadProgram([new ProgramCard(action, 0)]);
    const api = new BoardApiImpl(b, [r]);

    new Game(b, api, [r]).startRound();

    assertPosDir(r, 1, 1, expected);
  });

  it("push chain tail falls off right edge", () => {
    const b = initEmptyBoard(3, 3);
    const rs = lineRobots(1, 0, 0, 3, Direction.E);
    const api = new BoardApiImpl(b, rs);

    const out = api.tryMoveOneStep(1, Direction.E);

    const moved = assertMoved(out);
    expect(moved.destroys.length, "exactly one destroy (tail falls)").toBe(1);
    assertDestroy(moved, 0, 3, new Coord(3, 0));
    expect(moved.destroys[moved.destroys.length - 1].cause).toBe(
      DestroyCause.FELL_OFF,
    );

    expect(moved.moves.length, "R2 then R1").toBe(2);
    assertMove(moved, 0, 2, new Coord(1, 0), new Coord(2, 0));
    assertMove(moved, 1, 1, new Coord(0, 0), new Coord(1, 0));
  });

  it("wall on same tile blocks exit edge", () => {
    const b = initEmptyBoard(3, 3);
    walls(b, 0, 0, Direction.E);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r1]);

    const out = api.tryMoveOneStep(1, Direction.E);
    assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
  });

  it("wall on adjacent tile blocks entry edge", () => {
    const b = initEmptyBoard(3, 3);
    walls(b, 1, 0, Direction.W);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r1]);

    const out = api.tryMoveOneStep(1, Direction.E);
    assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
  });

  it("push chain blocked by wall on target edge", () => {
    const b = initEmptyBoard(3, 3);
    walls(b, 1, 0, Direction.E);
    const rs = lineRobots(1, 0, 0, 2, Direction.E);
    const api = new BoardApiImpl(b, rs);

    const out = api.tryMoveOneStep(1, Direction.E);
    assertChainBlockedByEdge(out, [2], new Edge(new Coord(1, 0), new Coord(2, 0)));
  });

  it("walls on board edge", () => {
    const b = initEmptyBoard(1, 1);
    walls(b, 0, 0, Direction.E);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r1]);

    const out = api.tryMoveOneStep(1, Direction.E);
    assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
  });

  it("fall off edge", () => {
    const b = initEmptyBoard(1, 1);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r1]);

    const out = api.tryMoveOneStep(1, Direction.E);
    const moved = assertMoved(out);
    assertDestroy(moved, 0, 1, new Coord(1, 0));
    expect(moved.destroys[moved.destroys.length - 1].cause).toBe(
      DestroyCause.FELL_OFF,
    );
  });

  it("walls on both tiles but non blocking across E", () => {
    const b = initEmptyBoard(3, 3);
    walls(b, 0, 0, Direction.S, Direction.N, Direction.W);
    walls(b, 1, 0, Direction.S, Direction.N, Direction.E);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r1]);

    const out = api.tryMoveOneStep(1, Direction.E);
    const moved = assertMoved(out);
    expect(moved.destroys.length, "no robots destroyed").toBe(0);
    expect(moved.moves.length, "one move event expected").toBe(1);
    assertMove(moved, 0, 1, new Coord(0, 0), new Coord(1, 0));
  });

  it("program MOVE -1 blocked by wall behind", () => {
    const b = initEmptyBoard(3, 1);
    walls(b, 0, 0, Direction.E);
    const r = new Robot(1, 1, 0, Direction.E);
    r.loadProgram([new ProgramCard(Action.MOVE, -1)]);
    const api = new BoardApiImpl(b, [r]);

    new Game(b, api, [r]).startRound();
    assertPosDir(r, 1, 0, Direction.E);
  });

  it("backward step off left edge is destroyed", () => {
    const b = initEmptyBoard(1, 1);
    const r = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r]);

    const out = api.tryMoveOneStep(1, Direction.W);
    const moved = assertMoved(out);

    expect(moved.moves.length, "no moves when immediately falling").toBe(0);
    expect(moved.destroys.length).toBe(1);
    assertDestroy(moved, 0, 1, new Coord(-1, 0));
    expect(moved.destroys[moved.destroys.length - 1].cause).toBe(
      DestroyCause.FELL_OFF,
    );
  });

  it("push chain simple two robots", () => {
    const b = initEmptyBoard(3, 1);
    const rs = lineRobots(1, 0, 0, 2, Direction.E);
    const api = new BoardApiImpl(b, rs);

    const out = api.tryMoveOneStep(1, Direction.E);
    const moved = assertMoved(out);

    expect(moved.destroys.length).toBe(0);
    expect(moved.moves.length).toBe(2);
    assertMove(moved, 0, 2, new Coord(1, 0), new Coord(2, 0));
    assertMove(moved, 1, 1, new Coord(0, 0), new Coord(1, 0));
  });

  it("push chain blocked by wall further down chain", () => {
    const b = initEmptyBoard(4, 1);
    walls(b, 2, 0, Direction.E);
    const rs = lineRobots(1, 0, 0, 3, Direction.E);
    const api = new BoardApiImpl(b, rs);

    const out = api.tryMoveOneStep(1, Direction.E);
    assertChainBlockedByEdge(
      out,
      [2, 3],
      new Edge(new Coord(2, 0), new Coord(3, 0)),
    );
  });

  it("two walls on crossing edge also blocks", () => {
    const b = initEmptyBoard(3, 1);
    walls(b, 0, 0, Direction.E);
    walls(b, 1, 0, Direction.W);
    const r1 = new Robot(1, 0, 0, Direction.E);
    const api = new BoardApiImpl(b, [r1]);

    const out = api.tryMoveOneStep(1, Direction.E);
    assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
  });

  it("program MOVE zero is noop", () => {
    const b = initEmptyBoard(3, 3);
    const r = new Robot(1, 1, 1, Direction.E);
    r.loadProgram([new ProgramCard(Action.MOVE, 0)]);
    const api = new BoardApiImpl(b, [r]);

    new Game(b, api, [r]).startRound();
    assertPosDir(r, 1, 1, Direction.E);
  });
});
