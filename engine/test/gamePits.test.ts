import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { Phase } from "../src/core/phase.js";
import { ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { DestroyCause } from "../src/rules/outcome.js";
import { Pits } from "../src/effects/pits.js";
import { initBoardWithRebootTokenAndPits } from "./util/boardTestUtils.js";
import { assertMoved, assertPosDir } from "./util/testSupport.js";

/** Ported from dk.dtu.GamePitsTest. */
describe("GamePits", () => {
  it("robot respawns after hit by pits", () => {
    const board = initBoardWithRebootTokenAndPits(5, 5);
    const r = new Robot(1, 0, 0, Direction.S);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r.loadProgram([ProgramCard.move1()]);
    game.executeRegister(1);

    expect(r.isAlive()).toBe(false);

    game.setRespawnDirection(1, Direction.E);
    game.applyRespawnPhase(r);

    expect(r.isAlive()).toBe(true);
    assertPosDir(r, 2, 2, Direction.E);
  });

  it("robot respawns after hit by pits and then pushed", () => {
    const board = initBoardWithRebootTokenAndPits(5, 5);
    const r1 = new Robot(1, 1, 1, Direction.W);
    const r2 = new Robot(2, 0, 2, Direction.N);
    const robots = [r1, r2];
    const api = new BoardApiImpl(board, robots);
    const game = new Game(board, api, robots);

    r1.loadProgram([ProgramCard.move1()]);
    r2.loadProgram([ProgramCard.move1()]);
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

  it("pits reports destroy with pits cause", () => {
    const board = initBoardWithRebootTokenAndPits(5, 5);
    const r = new Robot(1, 0, 1, Direction.S);
    const robots = [r];
    const api = new BoardApiImpl(board, robots);

    expect(r.isAlive()).toBe(true);

    const pitTile = board.getTile(0, 1);
    const pits = new Pits();
    pits.onPhase(Phase.ACTIVATE_PITS, pitTile, api);

    const out = api.resolveIntents();
    const moved = assertMoved(out);

    expect(moved.destroys.length).toBe(1);
    expect(moved.destroys[0].cause).toBe(DestroyCause.PITS);
    expect(moved.destroys[0].robotId).toBe(r.getId());
  });
});
