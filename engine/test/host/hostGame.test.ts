import { describe, it, expect } from "vitest";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { boardToSnapshot } from "../../src/host/snapshot.js";
import {
  createGame,
  submitProgram,
  allSubmitted,
  runActivation,
} from "../../src/host/hostGame.js";
import {
  initEmptyBoard,
  initBoardWithCheckPoints,
  initBoardWithGreenConveyors,
} from "../util/boardTestUtils.js";
import { prog, withHand, withProgram } from "../util/hostTestUtils.js";

describe("HostGame orchestrator", () => {
  it("createGame deals hands and starts in programming", () => {
    const board = boardToSnapshot(initEmptyBoard(5, 5));
    const snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 1, y: 1, facing: Direction.E },
    ]);

    expect(snap.status).toBe("programming");
    expect(snap.round).toBe(1);
    expect(snap.robots).toHaveLength(1);
    expect(snap.decks["1"].hand).toHaveLength(9);
    expect(snap.players[0].locked).toBe(false);
    expect(snap.winner).toBeNull();
    expect(snap.activation).toBeNull();
    expect(snap.pendingReaction).toBeNull();
  });

  it("submitProgram locks a player and allSubmitted flips", () => {
    const board = boardToSnapshot(initEmptyBoard(5, 5));
    let snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
    ]);
    expect(allSubmitted(snap)).toBe(false);

    snap = withHand(snap, 1, [
      ProgramCard.move1(),
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
    ]);
    snap = submitProgram(
      snap,
      1,
      prog(
        ProgramCard.move1(),
        ProgramCard.left(),
        ProgramCard.left(),
        ProgramCard.left(),
        ProgramCard.left(),
      ),
    );
    expect(snap.players[0].locked).toBe(true);
    expect(allSubmitted(snap)).toBe(true);
  });

  it("runActivation moves a robot per its program and advances the round", () => {
    // A board with checkpoints the robot never reaches, so all five registers
    // run (a zero-checkpoint board auto-wins after register 1, matching Java).
    const board = boardToSnapshot(initBoardWithCheckPoints(6, 6));
    let snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
    ]);
    const picked = [
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.right(),
    ];
    snap = withHand(snap, 1, picked);
    snap = submitProgram(snap, 1, prog(...picked));

    const { snapshot: next, frames } = runActivation(snap);

    const robot = next.robots.find((r) => r.id === 1)!;
    expect(robot.x).toBe(4);
    expect(robot.y).toBe(0);
    expect(robot.facing).toBe(Direction.S);

    expect(frames.length).toBeGreaterThan(1);
    expect(next.round).toBe(2);
    expect(next.status).toBe("programming");
    expect(next.players[0].locked).toBe(false);
    expect(next.players[0].program).toBeNull();
    expect(next.activation).toBeNull();
    expect(next.pendingReaction).toBeNull();
    // Mid-activation fields are absent once the round is over.
    expect(robot.registers).toBeUndefined();
    expect(robot.lastExecuted).toBeUndefined();
    expect(robot.movedOnActivation).toBeUndefined();
  });

  it("runActivation resolves a push between two robots", () => {
    const board = boardToSnapshot(initBoardWithCheckPoints(5, 5));
    let snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
      { robotId: 2, name: "Bo", color: "#0f0", x: 1, y: 0, facing: Direction.E },
    ]);
    snap = withProgram(snap, 1, [ProgramCard.move1()]);
    snap = withProgram(snap, 2, []);

    const { snapshot: next } = runActivation(snap);
    const r1 = next.robots.find((r) => r.id === 1)!;
    const r2 = next.robots.find((r) => r.id === 2)!;
    expect([r1.x, r1.y]).toEqual([1, 0]);
    expect([r2.x, r2.y]).toEqual([2, 0]);
  });

  it("a conveyor carries a robot with an empty program", () => {
    const board = boardToSnapshot(initBoardWithGreenConveyors(5, 5));
    let snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 1, y: 0, facing: Direction.E },
    ]);
    snap = withProgram(snap, 1, []);

    const { snapshot: next } = runActivation(snap);
    const robot = next.robots.find((r) => r.id === 1)!;
    expect(robot.x).toBe(2);
    expect(robot.y).toBe(4);
    expect(robot.facing).toBe(Direction.S);
  });

  it("declares a winner after checkpoints are reached in order", () => {
    const board = boardToSnapshot(initBoardWithCheckPoints(3, 3));
    let snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 1, facing: Direction.E },
    ]);

    // Round 1: reach checkpoint 1 at (1,1).
    snap = withProgram(snap, 1, [ProgramCard.move1()]);
    let result = runActivation(snap);
    snap = result.snapshot;
    expect(snap.robots[0].nextCheckpoint).toBe(2);
    expect(snap.winner).toBeNull();
    expect(snap.status).toBe("programming");
    expect(snap.round).toBe(2);

    // Round 2: reach checkpoint 2 at (2,2) to win.
    snap = withProgram(snap, 1, [
      ProgramCard.right(),
      ProgramCard.move1(),
      ProgramCard.left(),
      ProgramCard.move1(),
    ]);
    result = runActivation(snap);
    snap = result.snapshot;

    expect(snap.robots[0].x).toBe(2);
    expect(snap.robots[0].y).toBe(2);
    expect(snap.winner).toBe(1);
    expect(snap.status).toBe("finished");
    // The round only advances on the way back into programming.
    expect(snap.round).toBe(2);
  });

  it("runActivation refuses to restart a paused activation", () => {
    const board = boardToSnapshot(initBoardWithCheckPoints(5, 5));
    const snap = createGame(board, [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
    ]);
    expect(() =>
      runActivation({ ...snap, status: "awaiting-respawn" }),
    ).toThrowError(/cannot restart a paused activation/);
  });
});
