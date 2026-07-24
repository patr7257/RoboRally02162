import { describe, it, expect } from "vitest";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import {
  createGame,
  runActivation,
  applyRespawns,
} from "../../src/host/hostGame.js";
import {
  boardWithPitAndRebootToken,
  withProgram,
} from "../util/hostTestUtils.js";

/**
 * Reboot handling (issue #6): a round that killed robots stops before dealing
 * new hands, exactly like the Java scheduler, and waits for a facing per robot.
 * The board has a pit at (0,1) and a reboot token at (2,2) facing east.
 */

const board = () => boardWithPitAndRebootToken(6, 6);

/** Robot 1 starts above the pit facing south and drives into it. */
function fallInPit(extra: { robotId: number; x: number; y: number }[] = []) {
  let snap = createGame(board(), [
    { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.S },
    ...extra.map((e) => ({
      robotId: e.robotId,
      name: "Bo" + e.robotId,
      color: "#0f0",
      x: e.x,
      y: e.y,
      facing: Direction.N,
    })),
  ]);
  snap = withProgram(snap, 1, [ProgramCard.move1()]);
  for (const e of extra) snap = withProgram(snap, e.robotId, []);
  return snap;
}

describe("respawn pause and reboot", () => {
  it("a pit death ends the round in awaiting-respawn without dealing hands", () => {
    const snap = fallInPit();
    const handBefore = snap.decks["1"].hand;

    const { snapshot: down } = runActivation(snap);

    expect(down.status).toBe("awaiting-respawn");
    // The round only advances on the way back into programming.
    expect(down.round).toBe(1);
    expect(down.robots[0].alive).toBe(false);
    expect(down.decks["1"].hand).toEqual(handBefore);
    expect(down.activation).toBeNull();
    expect(down.pendingReaction).toBeNull();
  });

  it("applyRespawns puts the robot on the reboot token facing the choice", () => {
    const { snapshot: down } = runActivation(fallInPit());
    const { snapshot: next, frames } = applyRespawns(down, { 1: Direction.E });

    const robot = next.robots[0];
    expect([robot.x, robot.y]).toEqual([2, 2]);
    expect(robot.facing).toBe(Direction.E);
    expect(robot.alive).toBe(true);
    expect(robot.respawnDirection).toBeNull();

    expect(next.status).toBe("programming");
    expect(next.round).toBe(2);
    expect(next.decks["1"].hand).toHaveLength(9);
    expect(next.players[0].program).toBeNull();
    expect(next.players[0].locked).toBe(false);

    // Reboot frames are labelled for the animation layer (issue #7).
    const reboot = frames.filter((f) => f.label?.text === "REBOOT");
    expect(reboot.length).toBeGreaterThan(0);
    expect(reboot[0].label).toEqual({ robotId: 1, register: 0, text: "REBOOT" });
  });

  it("a missing direction keeps the facing the robot died with", () => {
    const { snapshot: down } = runActivation(fallInPit());
    // Robot 1 drove south into the pit, so it faces south at death.
    expect(down.robots[0].facing).toBe(Direction.S);

    const { snapshot: next } = applyRespawns(down, {});
    expect([next.robots[0].x, next.robots[0].y]).toEqual([2, 2]);
    expect(next.robots[0].facing).toBe(Direction.S);
  });

  it("a robot already on the token is pushed in the token direction", () => {
    // Robot 2 sits on the reboot token all round.
    const { snapshot: down } = runActivation(
      fallInPit([{ robotId: 2, x: 2, y: 2 }]),
    );
    expect(down.status).toBe("awaiting-respawn");

    const { snapshot: next } = applyRespawns(down, { 1: Direction.N });

    const r1 = next.robots.find((r) => r.id === 1)!;
    const r2 = next.robots.find((r) => r.id === 2)!;
    expect([r1.x, r1.y]).toEqual([2, 2]);
    expect(r1.facing).toBe(Direction.N);
    // Token faces east, so the occupant is pushed to (3,2).
    expect([r2.x, r2.y]).toEqual([3, 2]);
    expect(next.status).toBe("programming");
  });

  it("a paused respawn snapshot survives JSON", () => {
    const { snapshot: down } = runActivation(fallInPit());
    const viaJson = JSON.parse(JSON.stringify(down));

    const direct = applyRespawns(down, { 1: Direction.W });
    const restored = applyRespawns(viaJson, { 1: Direction.W });
    expect(restored.snapshot.robots).toEqual(direct.snapshot.robots);
  });

  it("applyRespawns refuses a snapshot that is not awaiting a respawn", () => {
    const snap = fallInPit();
    expect(() => applyRespawns(snap, {})).toThrowError(/No respawns to apply/);
  });
});
