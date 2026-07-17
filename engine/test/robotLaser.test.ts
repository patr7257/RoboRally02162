import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { Phase } from "../src/core/phase.js";
import { Action } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { DamageDecks } from "../src/model/damageDecks.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { RobotLaser } from "../src/effects/robotLaser.js";
import { initBoardWithRobotLasers } from "./util/boardTestUtils.js";
import { assertPosDir } from "./util/testSupport.js";

/** Ported from dk.dtu.RobotLaserTest. */
describe("RobotLaser", () => {
  it("two robots facing each other both deal damage", () => {
    const board = initBoardWithRobotLasers(10, 10);
    const robot1 = new Robot(1, 2, 5, Direction.E);
    const robot2 = new Robot(2, 5, 5, Direction.W);
    const api = new BoardApiImpl(board, [robot1, robot2]);
    const game = new Game(board, api, [robot1, robot2]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    expect(game.getRobotDiscard(1).length).toBe(0);
    expect(game.getRobotDiscard(2).length).toBe(0);

    game.applyTileEffects(Phase.ACTIVATE_ROBOT_LASERS);

    const discard1 = game.getRobotDiscard(1);
    const discard2 = game.getRobotDiscard(2);
    expect(discard1.length).toBe(1);
    expect(discard2.length).toBe(1);
    expect(discard1.some((c) => c.action === Action.SPAM)).toBe(true);
    expect(discard2.some((c) => c.action === Action.SPAM)).toBe(true);
    assertPosDir(robot1, 2, 5, Direction.E);
    assertPosDir(robot2, 5, 5, Direction.W);
  });

  it("robot laser does not hit itself", () => {
    const board = initBoardWithRobotLasers(10, 10);
    const robot = new Robot(1, 3, 3, Direction.N);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    expect(game.getRobotDiscard(1).length).toBe(0);
    game.applyTileEffects(Phase.ACTIVATE_ROBOT_LASERS);
    expect(game.getRobotDiscard(1).length).toBe(0);
    assertPosDir(robot, 3, 3, Direction.N);
  });

  it("robot laser effects not present before activation", () => {
    const board = initBoardWithRobotLasers(10, 10);
    const robot1 = new Robot(1, 2, 2, Direction.E);
    const robot2 = new Robot(2, 5, 5, Direction.W);
    const api = new BoardApiImpl(board, [robot1, robot2]);
    void new Game(board, api, [robot1, robot2]);

    const tile1 = board.getTile(2, 2);
    const tile2 = board.getTile(5, 5);
    expect(tile1.getEffects().filter((e) => e instanceof RobotLaser).length).toBe(0);
    expect(tile2.getEffects().filter((e) => e instanceof RobotLaser).length).toBe(0);
  });

  it("robot laser effects added during activation and removed after", () => {
    const board = initBoardWithRobotLasers(10, 10);
    const robot1 = new Robot(1, 2, 3, Direction.E);
    const robot2 = new Robot(2, 5, 3, Direction.W);
    const api = new BoardApiImpl(board, [robot1, robot2]);
    const game = new Game(board, api, [robot1, robot2]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    const tile1 = board.getTile(2, 3);
    const tile2 = board.getTile(5, 3);
    expect(tile1.getEffects().filter((e) => e instanceof RobotLaser).length).toBe(0);
    expect(tile2.getEffects().filter((e) => e instanceof RobotLaser).length).toBe(0);

    game.runPhase(Phase.ACTIVATION, () => {});

    expect(tile1.getEffects().filter((e) => e instanceof RobotLaser).length).toBe(0);
    expect(tile2.getEffects().filter((e) => e instanceof RobotLaser).length).toBe(0);

    expect(game.getRobotDiscard(1).length).toBe(1);
    expect(game.getRobotDiscard(2).length).toBe(1);
    expect(game.getRobotDiscard(1).some((c) => c.action === Action.SPAM)).toBe(true);
    expect(game.getRobotDiscard(2).some((c) => c.action === Action.SPAM)).toBe(true);
  });

  it("dead robots do not get laser effects", () => {
    const board = initBoardWithRobotLasers(10, 10);
    const robot1 = new Robot(1, 2, 2, Direction.E);
    const robot2 = new Robot(2, 5, 5, Direction.W);
    robot2.setDead();
    const api = new BoardApiImpl(board, [robot1, robot2]);
    const game = new Game(board, api, [robot1, robot2]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    game.runPhase(Phase.ACTIVATION, () => {});

    expect(game.getRobotDiscard(1).length).toBe(0);
  });

  it("robot laser effects only active in correct phase", () => {
    const robot = new Robot(1, 2, 2, Direction.E);
    const laserEffect = new RobotLaser(robot);
    const phases = laserEffect.phases()!;

    expect(phases.has(Phase.ACTIVATE_ROBOT_LASERS)).toBe(true);
    expect(phases.size).toBe(1);
    expect(phases.has(Phase.ACTIVATE_BOARD_LASERS)).toBe(false);
    expect(phases.has(Phase.ACTIVATE_CHECKPOINTS)).toBe(false);
    expect(phases.has(Phase.ACTIVATE_GEAR)).toBe(false);
  });
});
