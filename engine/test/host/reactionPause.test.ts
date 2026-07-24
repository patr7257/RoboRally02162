import { describe, it, expect } from "vitest";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import {
  createGame,
  submitProgram,
  runActivation,
  resumeActivation,
  REACTION_SPECS,
} from "../../src/host/hostGame.js";
import type { ReactionChoice } from "../../src/host/hostGame.js";
import {
  boardWithFarCheckpoint,
  prog,
  throughJson,
  withDrawPile,
  withHand,
  withProgram,
} from "../util/hostTestUtils.js";

/**
 * Interactive cards (issues #5 / #6): the host stops mid-activation, serializes
 * the pause, and resumes from the snapshot without re-executing anything.
 */

const board = () => boardWithFarCheckpoint(10, 6);

function gameWithOneRobot() {
  return createGame(board(), [
    { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
  ]);
}

describe("reaction pause and resume", () => {
  it("SANDBOX at register 3 pauses with prompt, cursor and serialized registers", () => {
    const picked = [
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.sandbox(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    let snap = gameWithOneRobot();
    snap = withHand(snap, 1, picked);
    snap = submitProgram(snap, 1, prog(...picked));
    const handBefore = snap.decks["1"].hand;

    const { snapshot: paused } = runActivation(snap);

    expect(paused.status).toBe("awaiting-reaction");
    // The round only advances on the way back into programming.
    expect(paused.round).toBe(1);
    expect(paused.winner).toBeNull();

    expect(paused.pendingReaction).toEqual({
      promptId: "r1-g3-t0-1",
      robotId: 1,
      register: 3,
      kind: "SANDBOX",
      options: ["MOVE1", "MOVE2", "MOVE3", "BACKUP", "LEFT", "RIGHT", "UTURN"],
      defaultChoice: "MOVE1",
    });
    expect(paused.activation).toEqual({
      register: 3,
      turnOrder: [1],
      turnIndex: 0,
    });

    const robot = paused.robots[0];
    // Two registers played, the SANDBOX card consumed, two still queued.
    expect([robot.x, robot.y]).toEqual([2, 0]);
    expect(robot.registers).toEqual(prog(ProgramCard.move1(), ProgramCard.move1()));
    expect(robot.lastExecuted).toEqual({ action: "MOVE", steps: 1 });
    expect(robot.movedOnActivation).toBe(false);

    // No new hands while paused.
    expect(paused.decks["1"].hand).toEqual(handBefore);
  });

  it("resumeActivation with MOVE2 moves two and completes the round", () => {
    const picked = [
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.sandbox(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    let snap = gameWithOneRobot();
    snap = withHand(snap, 1, picked);
    snap = submitProgram(snap, 1, prog(...picked));

    const { snapshot: paused } = runActivation(snap);
    const { snapshot: done, frames } = resumeActivation(paused, "MOVE2");

    // (2,0) + MOVE2 + MOVE1 + MOVE1.
    expect([done.robots[0].x, done.robots[0].y]).toEqual([6, 0]);
    expect(done.status).toBe("programming");
    expect(done.round).toBe(2);
    expect(done.activation).toBeNull();
    expect(done.pendingReaction).toBeNull();
    expect(done.players[0].program).toBeNull();
    expect(done.players[0].locked).toBe(false);
    expect(done.decks["1"].hand).toHaveLength(9);
    expect(frames.length).toBeGreaterThan(1);
    // Frames cover only this segment: they start at the paused position.
    expect([frames[0].robots[0].x, frames[0].robots[0].y]).toEqual([2, 0]);
  });

  it("a missing choice falls back to the SANDBOX default MOVE1", () => {
    let snap = gameWithOneRobot();
    snap = withProgram(snap, 1, [ProgramCard.sandbox()]);

    const { snapshot: paused } = runActivation(snap);
    const { snapshot: done } = resumeActivation(paused);

    expect(REACTION_SPECS.SANDBOX.defaultChoice).toBe("MOVE1");
    expect([done.robots[0].x, done.robots[0].y]).toEqual([1, 0]);
    expect(done.status).toBe("programming");
  });

  it("WEASEL offers the three rotations and defaults to LEFT", () => {
    let snap = gameWithOneRobot();
    snap = withProgram(snap, 1, [ProgramCard.weasel()]);

    const { snapshot: paused } = runActivation(snap);
    expect(paused.pendingReaction).toEqual({
      promptId: "r1-g1-t0-1",
      robotId: 1,
      register: 1,
      kind: "WEASEL",
      options: ["LEFT", "RIGHT", "UTURN"],
      defaultChoice: "LEFT",
    });

    // No choice: default LEFT, so east becomes north.
    expect(resumeActivation(paused).snapshot.robots[0].facing).toBe(Direction.N);
    // A choice that is not legal for WEASEL also falls back to LEFT.
    const illegal = "MOVE3" as ReactionChoice;
    expect(resumeActivation(paused, illegal).snapshot.robots[0].facing).toBe(
      Direction.N,
    );
    // A legal choice is honoured.
    expect(resumeActivation(paused, "UTURN").snapshot.robots[0].facing).toBe(
      Direction.W,
    );
  });

  it("SPEED auto-resolves to MOVE3 without pausing", () => {
    let snap = gameWithOneRobot();
    snap = withProgram(snap, 1, [ProgramCard.speed()]);

    const { snapshot: done, frames } = runActivation(snap);

    expect(REACTION_SPECS.SPEED.options).toEqual(["MOVE3"]);
    expect(done.status).toBe("programming");
    expect(done.round).toBe(2);
    expect(done.pendingReaction).toBeNull();
    expect([done.robots[0].x, done.robots[0].y]).toEqual([3, 0]);
    expect(frames.some((f) => f.label?.text === "MOVE3")).toBe(true);
  });

  it("a SPAM resolution that flips a reaction pauses on the flipped card", () => {
    let snap = gameWithOneRobot();
    snap = withProgram(snap, 1, [ProgramCard.spam()]);
    snap = withHand(snap, 1, [ProgramCard.spam(), ProgramCard.spam()]);
    snap = withDrawPile(snap, 1, [
      ProgramCard.sandbox(),
      ProgramCard.move1(),
      ProgramCard.left(),
    ]);
    const spamPoolBefore = snap.damageDecks.spam;

    const { snapshot: paused } = runActivation(snap);

    expect(paused.status).toBe("awaiting-reaction");
    expect(paused.pendingReaction?.kind).toBe("SANDBOX");
    expect(paused.pendingReaction?.promptId).toBe("r1-g1-t0-1");

    // The flipped card really left the draw pile and went to the discard, and
    // the SPAM card left the hand and went back to the global pool.
    const deck = paused.decks["1"];
    expect(deck.drawPile).toEqual(prog(ProgramCard.move1(), ProgramCard.left()));
    expect(deck.discardPile).toEqual(prog(ProgramCard.sandbox()));
    expect(deck.hand).toEqual(prog(ProgramCard.spam()));
    expect(paused.damageDecks.spam).toBe(spamPoolBefore + 1);

    const { snapshot: done } = resumeActivation(paused, "RIGHT");
    expect([done.robots[0].x, done.robots[0].y]).toEqual([0, 0]);
    expect(done.robots[0].facing).toBe(Direction.S);
    expect(done.robots[0].alive).toBe(true);
    expect(done.status).toBe("programming");
  });

  it("a paused snapshot survives JSON and resumes identically", () => {
    const picked = [
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.sandbox(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    let snap = gameWithOneRobot();
    snap = withHand(snap, 1, picked);
    snap = submitProgram(snap, 1, prog(...picked));

    const { snapshot: paused } = runActivation(snap);
    const direct = resumeActivation(paused, "MOVE3");
    const viaJson = resumeActivation(throughJson(paused), "MOVE3");

    expect(viaJson.snapshot).toEqual(direct.snapshot);
    expect(viaJson.frames).toEqual(direct.frames);
  });

  it("two reactions in one activation resolve one after the other", () => {
    let snap = gameWithOneRobot();
    snap = withProgram(snap, 1, [ProgramCard.sandbox(), ProgramCard.sandbox()]);

    const first = runActivation(snap);
    expect(first.snapshot.pendingReaction?.promptId).toBe("r1-g1-t0-1");

    const second = resumeActivation(first.snapshot, "MOVE2");
    expect(second.snapshot.status).toBe("awaiting-reaction");
    expect(second.snapshot.pendingReaction?.promptId).toBe("r1-g2-t0-1");
    expect(second.snapshot.activation).toEqual({
      register: 2,
      turnOrder: [1],
      turnIndex: 0,
    });
    expect([second.snapshot.robots[0].x, second.snapshot.robots[0].y]).toEqual([
      2, 0,
    ]);
    expect(second.snapshot.round).toBe(1);

    const third = resumeActivation(second.snapshot, "RIGHT");
    expect(third.snapshot.status).toBe("programming");
    expect(third.snapshot.round).toBe(2);
    expect(third.snapshot.robots[0].facing).toBe(Direction.S);
    expect([third.snapshot.robots[0].x, third.snapshot.robots[0].y]).toEqual([
      2, 0,
    ]);
  });

  it("AGAIN after a resolved reaction repeats the chosen op", () => {
    let snap = gameWithOneRobot();
    snap = withProgram(snap, 1, [
      ProgramCard.move2(),
      ProgramCard.sandbox(),
      ProgramCard.again(),
    ]);

    const { snapshot: paused } = runActivation(snap);
    // The op played before the pause is carried in the snapshot, so AGAIN still
    // works after a resume.
    expect(paused.robots[0].lastExecuted).toEqual({ action: "MOVE", steps: 2 });
    expect(paused.robots[0].registers).toEqual(prog(ProgramCard.again()));

    const { snapshot: done } = resumeActivation(throughJson(paused), "MOVE2");
    // MOVE2 (register 1) + MOVE2 (chosen) + MOVE2 (AGAIN).
    expect([done.robots[0].x, done.robots[0].y]).toEqual([6, 0]);
    expect(done.status).toBe("programming");
  });

  it("resumeActivation refuses a snapshot with no pending reaction", () => {
    const snap = gameWithOneRobot();
    expect(() => resumeActivation(snap, "MOVE1")).toThrowError(
      /No pending reaction/,
    );
  });
});
