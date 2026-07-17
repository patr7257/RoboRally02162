import { describe, it, expect } from "vitest";
import { Board } from "../src/model/board.js";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { Phase } from "../src/core/phase.js";
import { Action, ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { Deck } from "../src/model/deck.js";
import { DamageDecks } from "../src/model/damageDecks.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import { BoardLaser } from "../src/effects/boardLaser.js";
import { Antenna } from "../src/effects/antenna.js";
import { Walls } from "../src/effects/walls.js";
import { initEmptyCells } from "./util/boardTestUtils.js";
import { assertPosDir } from "./util/testSupport.js";

function spamCount(cards: ProgramCard[]): number {
  return cards.filter((c) => c.action === Action.SPAM).length;
}

/** Ported from dk.dtu.BoardLaserTest. */
describe("BoardLaser", () => {
  it("board laser on robot deals damage", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[2][1].setEffects([new BoardLaser(Direction.S, 1)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 2, 2, Direction.N);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    expect(game.getRobotDiscard(1).length).toBe(0);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    const discardAfter = game.getRobotDiscard(1);
    expect(discardAfter.length).toBe(1);
    expect(discardAfter.some((c) => c.action === Action.SPAM)).toBe(true);
    assertPosDir(robot, 2, 2, Direction.N);
  });

  it("power equals spam card count", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[3][1].setEffects([new BoardLaser(Direction.S, 3)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 3, 3, Direction.E);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    expect(game.getRobotDiscard(1).length).toBe(0);
    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    const discardAfter = game.getRobotDiscard(1);
    expect(discardAfter.length).toBe(3);
    expect(spamCount(discardAfter)).toBe(3);
    assertPosDir(robot, 3, 3, Direction.E);
  });

  it("laser hits wall and stops", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[1][1].setEffects([new BoardLaser(Direction.S, 2)]);
    tiles[1][2].setEffects([new Walls([Direction.N])]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 1, 3, Direction.S);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    expect(game.getRobotDiscard(1).length).toBe(0);
    assertPosDir(robot, 1, 3, Direction.S);
  });

  it("laser hits max one robot", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[2][5].setEffects([new BoardLaser(Direction.E, 2)]);
    const board = new Board(10, 10, tiles);

    const robot1 = new Robot(1, 4, 5, Direction.W);
    const robot2 = new Robot(2, 6, 5, Direction.W);
    const api = new BoardApiImpl(board, [robot1, robot2]);
    const game = new Game(board, api, [robot1, robot2]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    expect(game.getRobotDiscard(1).length).toBe(0);
    expect(game.getRobotDiscard(2).length).toBe(0);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    expect(game.getRobotDiscard(1).length).toBe(2);
    expect(game.getRobotDiscard(2).length).toBe(0);
    expect(spamCount(game.getRobotDiscard(1))).toBe(2);
    assertPosDir(robot1, 4, 5, Direction.W);
    assertPosDir(robot2, 6, 5, Direction.W);
  });

  it("robot on laser tile is hit", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[2][1].setEffects([new BoardLaser(Direction.S, 1)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 2, 1, Direction.N);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    expect(game.getRobotDiscard(1).length).toBe(0);
    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    const discardAfter = game.getRobotDiscard(1);
    expect(discardAfter.length).toBe(1);
    expect(discardAfter.some((c) => c.action === Action.SPAM)).toBe(true);
    assertPosDir(robot, 2, 1, Direction.N);
  });

  it("robot takes damage each round", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[2][1].setEffects([new BoardLaser(Direction.S, 1)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 2, 2, Direction.E);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);
    game.setDamageDecks(new DamageDecks(38, 15, 15));

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
    expect(game.getRobotDiscard(1).length).toBe(1);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
    expect(game.getRobotDiscard(1).length).toBe(2);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
    const discardAfterRound3 = game.getRobotDiscard(1);
    expect(discardAfterRound3.length).toBe(3);
    expect(spamCount(discardAfterRound3)).toBe(3);
    assertPosDir(robot, 2, 2, Direction.E);
  });

  it("laser blocked by antenna", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[5][5].setEffects([new BoardLaser(Direction.E, 2)]);
    tiles[7][5].setEffects([new Antenna(Direction.N)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 8, 5, Direction.W);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    expect(game.getRobotDiscard(1).length).toBe(0);
    assertPosDir(robot, 8, 5, Direction.W);
  });

  it("wall and robot same tile wall blocks laser when facing opposite", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[3][5].setEffects([new BoardLaser(Direction.E, 1)]);
    tiles[5][5].setEffects([new Walls([Direction.W])]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 5, 5, Direction.N);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    expect(game.getRobotDiscard(1).length).toBe(0);
    assertPosDir(robot, 5, 5, Direction.N);
  });

  it("wall and robot same tile wall blocks laser when facing non opposite", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[3][5].setEffects([new BoardLaser(Direction.E, 1)]);
    tiles[5][5].setEffects([
      new Walls([Direction.N, Direction.S, Direction.E]),
    ]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 5, 5, Direction.N);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);

    const discardAfter = game.getRobotDiscard(1);
    expect(discardAfter.length).toBe(1);
    expect(discardAfter.some((c) => c.action === Action.SPAM)).toBe(true);
    assertPosDir(robot, 5, 5, Direction.N);
  });

  it("robot on laser moves out and back is hit only once", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[2][1].setEffects([new BoardLaser(Direction.S, 1)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 2, 2, Direction.W);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    const hand = [
      ProgramCard.uturn(),
      ProgramCard.back1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    const deck = new Deck([], [], hand, new DamageDecks(38, 15, 15));
    game.setDeck(deck, 1);

    game.submitProgram(
      1,
      [
        ProgramCard.uturn(),
        ProgramCard.back1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
      ],
      false,
    );

    game.executeRegister(1);
    assertPosDir(robot, 2, 2, Direction.E);
    expect(game.getRobotDiscard(1).length).toBe(1);

    game.executeRegister(2);
    assertPosDir(robot, 1, 2, Direction.E);
    expect(game.getRobotDiscard(1).length).toBe(1);

    game.executeRegister(3);
    assertPosDir(robot, 2, 2, Direction.E);
    const discardAfterRegister3 = game.getRobotDiscard(1);
    expect(discardAfterRegister3.length).toBe(2);
    expect(spamCount(discardAfterRegister3)).toBe(2);
  });

  it("dont deal damage when robot moves away from laser field", () => {
    const tiles = initEmptyCells(10, 10);
    tiles[2][1].setEffects([new BoardLaser(Direction.S, 1)]);
    const board = new Board(10, 10, tiles);

    const robot = new Robot(1, 2, 2, Direction.W);
    const api = new BoardApiImpl(board, [robot]);
    const game = new Game(board, api, [robot]);

    const hand = [
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    const deck = new Deck([], [], hand, new DamageDecks(38, 15, 15));
    game.setDeck(deck, 1);

    game.submitProgram(
      1,
      [
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
      ],
      false,
    );

    game.executeRegister(1);
    assertPosDir(robot, 1, 2, Direction.W);
    expect(game.getRobotDiscard(1).length).toBe(0);
    expect(spamCount(game.getRobotDiscard(1))).toBe(0);
  });
});
