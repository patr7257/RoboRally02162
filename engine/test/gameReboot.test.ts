import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { initBoardWithRebootToken } from "./util/boardTestUtils.js";
import { assertPosDir } from "./util/testSupport.js";

/** Ported from dk.dtu.GameRebootTest. */
describe("GameReboot", () => {
  it("robot respawn", () => {
    const board = initBoardWithRebootToken(5, 5);
    const r = new Robot(1, 0, 0, Direction.N);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([ProgramCard.move3()]);
    game.executeRegister(1);

    expect(r.isAlive()).toBe(false);

    game.setRespawnDirection(1, Direction.E);
    game.applyRespawnPhase(r);

    expect(r.isAlive()).toBe(true);
    assertPosDir(r, 2, 2, Direction.E);
  });

  it("robot respawn and then pushed", () => {
    const board = initBoardWithRebootToken(5, 5);
    const r1 = new Robot(1, 0, 0, Direction.N);
    const r2 = new Robot(2, 1, 0, Direction.N);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r1.loadProgram([ProgramCard.move3()]);
    r2.loadProgram([ProgramCard.move3()]);
    game.executeRegister(1);

    expect(r1.isAlive()).toBe(false);
    expect(r2.isAlive()).toBe(false);

    game.setRespawnDirection(1, Direction.E);
    game.setRespawnDirection(2, Direction.E);

    game.applyRespawnPhase(r1);
    expect(r1.isAlive()).toBe(true);
    game.applyRespawnPhase(r2);
    expect(r2.isAlive()).toBe(true);

    assertPosDir(r2, 2, 2, Direction.E);
    assertPosDir(r1, 3, 2, Direction.E);
  });

  it("three robot respawn and then pushed", () => {
    const board = initBoardWithRebootToken(5, 5);
    const r1 = new Robot(1, 0, 0, Direction.N);
    const r2 = new Robot(2, 1, 0, Direction.N);
    const r3 = new Robot(3, 2, 0, Direction.N);
    const robots = [r1, r2, r3];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r1.loadProgram([ProgramCard.move3()]);
    r2.loadProgram([ProgramCard.move3()]);
    r3.loadProgram([ProgramCard.move3()]);
    game.executeRegister(1);

    expect(r1.isAlive()).toBe(false);
    expect(r2.isAlive()).toBe(false);
    expect(r3.isAlive()).toBe(false);

    game.setRespawnDirection(1, Direction.E);
    game.setRespawnDirection(2, Direction.E);
    game.setRespawnDirection(3, Direction.E);

    game.applyRespawnPhase(r1);
    expect(r1.isAlive()).toBe(true);
    game.applyRespawnPhase(r2);
    expect(r2.isAlive()).toBe(true);
    game.applyRespawnPhase(r3);
    expect(r3.isAlive()).toBe(true);

    assertPosDir(r3, 2, 2, Direction.E);
    assertPosDir(r2, 3, 2, Direction.E);
    assertPosDir(r1, 4, 2, Direction.E);
  });
});
