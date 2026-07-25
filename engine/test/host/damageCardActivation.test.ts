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
  withDrawPile,
  withHand,
  withProgram,
} from "../util/hostTestUtils.js";

/**
 * Host-level round trip for damage cards (issue #8): a programmed SPAM/WORM
 * pauses and resumes exactly like the pit-death path already covered by
 * respawn.test.ts, and the deck/pool snapshot fields (issue #14's data, plus
 * the global damageDecks pools) reflect the resolution correctly. The board
 * has a reboot token at (2,2) facing east; robot 1 starts away from any pit
 * so only the programmed card kills it.
 */

const board = () => boardWithPitAndRebootToken(6, 6);

function game() {
  return createGame(board(), [
    { robotId: 1, name: "Ada", color: "#f00", x: 3, y: 3, facing: Direction.N },
  ]);
}

describe("damage card activation (host)", () => {
  it("a programmed WORM ends the round in awaiting-respawn, returns WORM to the pool, and clears the hand", () => {
    let snap = game();
    snap = withHand(snap, 1, [
      ProgramCard.worm(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ]);
    // Deliberately NOT overriding the draw pile: WORM never touches it, and
    // leaving createGame's default (13 cards for a single robot) means the
    // later dealNewHands in applyRespawns has enough cards to redeal 9
    // without needing a reshuffle mid-draw.
    snap = withProgram(snap, 1, [ProgramCard.worm()]);
    const poolBefore = snap.damageDecks.worm;

    const { snapshot: down } = runActivation(snap);

    expect(down.status).toBe("awaiting-respawn");
    expect(down.robots[0].alive).toBe(false);
    // WORM left the hand (removeFromHand) and was returned to the pool
    // (putBack), matching Game.executeOneRobotTurn's WORM branch.
    expect(down.decks["1"].hand.some((c) => c.action === "WORM")).toBe(false);
    expect(down.damageDecks.worm).toBe(poolBefore + 1);
  });

  it("applyRespawns revives a WORM-killed robot on the reboot token like any other death", () => {
    let snap = game();
    snap = withHand(snap, 1, [
      ProgramCard.worm(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ]);
    snap = withProgram(snap, 1, [ProgramCard.worm()]);

    const { snapshot: down } = runActivation(snap);
    const { snapshot: next } = applyRespawns(down, { 1: Direction.E });

    const robot = next.robots[0];
    expect([robot.x, robot.y]).toEqual([2, 2]);
    expect(robot.facing).toBe(Direction.E);
    expect(robot.alive).toBe(true);
    expect(next.status).toBe("programming");
    expect(next.decks["1"].hand).toHaveLength(9);
    // The revived hand must not still carry the WORM that was already
    // returned to the pool before the round paused.
    expect(next.decks["1"].hand.filter((c) => c.action === "WORM")).toHaveLength(0);
  });

  it("a programmed SPAM plays the top of the deck and returns SPAM to the pool without pausing", () => {
    let snap = game();
    snap = withHand(snap, 1, [ProgramCard.spam()]);
    snap = withDrawPile(snap, 1, [ProgramCard.move1()]);
    snap = withProgram(snap, 1, [ProgramCard.spam()]);
    const poolBefore = snap.damageDecks.spam;

    const { snapshot: next } = runActivation(snap);

    expect(next.status).toBe("programming");
    expect(next.robots[0].alive).toBe(true);
    // Moved one step north (facing N) by the top-of-deck MOVE1.
    expect([next.robots[0].x, next.robots[0].y]).toEqual([3, 2]);
    expect(next.damageDecks.spam).toBe(poolBefore + 1);
    expect(next.decks["1"].hand.some((c) => c.action === "SPAM")).toBe(false);
  });
});
