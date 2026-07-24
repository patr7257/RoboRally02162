import { describe, it, expect } from "vitest";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { createGame, runActivation } from "../../src/host/hostGame.js";
import { boardWithFarCheckpoint, withProgram } from "../util/hostTestUtils.js";

/** Frame labels (issue #7): every frame says who acted and with which card. */

describe("frame labels", () => {
  it("per-turn frames carry the acting robot, register and card name", () => {
    let snap = createGame(boardWithFarCheckpoint(8, 8), [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
      { robotId: 2, name: "Bo", color: "#0f0", x: 0, y: 3, facing: Direction.E },
    ]);
    snap = withProgram(snap, 1, [ProgramCard.move2(), ProgramCard.right()]);
    snap = withProgram(snap, 2, [ProgramCard.left()]);

    const { frames } = runActivation(snap);

    // The very first frame is the pre-activation position, with no label.
    expect(frames[0].label).toBeUndefined();

    expect(frames.map((f) => f.label).filter(Boolean)).toContainEqual({
      robotId: 1,
      register: 1,
      text: ProgramCard.move2().toString(),
    });
    expect(frames.map((f) => f.label).filter(Boolean)).toContainEqual({
      robotId: 2,
      register: 1,
      text: ProgramCard.left().toString(),
    });
    expect(frames.map((f) => f.label).filter(Boolean)).toContainEqual({
      robotId: 1,
      register: 2,
      text: ProgramCard.right().toString(),
    });

    // Card names are exactly ProgramCard.toString().
    expect(ProgramCard.move2().toString()).toBe("MOVE2");
    expect(ProgramCard.right().toString()).toBe("ROTATERIGHT");
  });

  it("end-of-register frames are labelled BOARD, one per register", () => {
    let snap = createGame(boardWithFarCheckpoint(8, 8), [
      { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
    ]);
    snap = withProgram(snap, 1, [ProgramCard.move1()]);

    const { frames } = runActivation(snap);

    const boardFrames = frames.filter((f) => f.label?.text === "BOARD");
    expect(boardFrames).toHaveLength(5);
    expect(boardFrames.map((f) => f.label!.register)).toEqual([1, 2, 3, 4, 5]);
    for (const f of boardFrames) expect(f.label!.robotId).toBeNull();
  });
});
