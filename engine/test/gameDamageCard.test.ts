import { describe, it, expect } from "vitest";
import { Robot } from "../src/model/robot.js";
import { Direction } from "../src/model/direction.js";
import { Action, ProgramCard } from "../src/program/programCard.js";
import { Game } from "../src/core/game.js";
import { Deck } from "../src/model/deck.js";
import { DamageDecks } from "../src/model/damageDecks.js";
import { BoardApiImpl } from "../src/rules/boardApi.js";
import {
  initEmptyBoard,
  initBoardWithRebootTokenAndPits,
} from "./util/boardTestUtils.js";
import { assertPosDir } from "./util/testSupport.js";

/** Ported from dk.dtu.GameDamageCardTest. */
describe("GameDamageCard", () => {
  it("damage cards stay in hand", () => {
    const board = initEmptyBoard(3, 3);
    const r = new Robot(1, 1, 1, Direction.E);
    const api = new BoardApiImpl(board, [r]);

    const drawPile: ProgramCard[] = [];
    for (let i = 0; i < 7; i++) drawPile.push(ProgramCard.move1());
    const hand = [
      ProgramCard.spam(),
      ProgramCard.trojanHorse(),
      ProgramCard.worm(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    const deck = new Deck(drawPile, [], hand, new DamageDecks(38, 15, 15));

    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);
    game.dealNewHands();

    const newHand = game.getRobotHand(1);
    expect(newHand.some((c) => c.action === Action.SPAM)).toBe(true);
    expect(newHand.some((c) => c.action === Action.TROJAN_HORSE)).toBe(true);
    expect(newHand.some((c) => c.action === Action.WORM)).toBe(true);
    expect(newHand.length).toBe(9);
  });

  it("spam card plays top card", () => {
    const board = initEmptyBoard(5, 5);
    const r = new Robot(1, 1, 1, Direction.E);
    const api = new BoardApiImpl(board, [r]);

    const drawPile = [ProgramCard.move1()];
    const deck = new Deck(drawPile, [], [], new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);

    r.loadProgram([ProgramCard.spam()]);
    game.executeOneRobotTurn(r);

    expect(r.getX()).toBe(2);
    expect(r.getY()).toBe(1);
    expect(game.getRobotDiscard(1).some((c) => c.action === Action.MOVE)).toBe(true);
  });

  it("lands on pits reboot", () => {
    const board = initBoardWithRebootTokenAndPits(5, 5);
    const r = new Robot(1, 0, 0, Direction.S);
    const api = new BoardApiImpl(board, [r]);

    const deck = new Deck([], [], [], new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);

    r.loadProgram([ProgramCard.move1()]);
    game.executeRegister(1);

    assertPosDir(r, 0, 1, Direction.S);
    expect(r.isAlive()).toBe(false);
    expect(game.getRobotDiscard(1).some((c) => c.action === Action.SPAM)).toBe(true);
  });

  it("lands on pits reboot with trojan", () => {
    const board = initBoardWithRebootTokenAndPits(5, 5);
    const r = new Robot(1, 0, 0, Direction.S);
    const api = new BoardApiImpl(board, [r]);

    const deck = new Deck([], [], [], new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);
    game.setDamageDecks(new DamageDecks(0, 15, 15));

    r.loadProgram([ProgramCard.move1()]);
    game.executeRegister(1);

    assertPosDir(r, 0, 1, Direction.S);
    expect(r.isAlive()).toBe(false);
    expect(
      game
        .getRobotDiscard(1)
        .some(
          (c) =>
            c.action === Action.TROJAN_HORSE || c.action === Action.WORM,
        ),
    ).toBe(true);
  });

  it("trojan horse with enough spam adds 2 spam to discard", () => {
    const board = initEmptyBoard(10, 10);
    const robot = new Robot(1, 0, 0, Direction.N);
    const api = new BoardApiImpl(board, [robot]);

    const drawPile = [ProgramCard.left()];
    const deck = new Deck(drawPile, [], [], new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [robot]);
    game.setDeck(deck, 1);
    game.setDamageDecks(new DamageDecks(5, 15, 15));

    robot.loadProgram([ProgramCard.trojanHorse()]);
    game.executeOneRobotTurn(robot);

    const discardPile = game.getRobotDiscard(1);
    expect(discardPile.filter((c) => c.action === Action.SPAM).length).toBe(2);
    expect(game.getDamageDecks().getSpamDrawPile()).toBe(3);
    expect(game.getDamageDecks().getTrojanHorseDrawPile()).toBe(16);
  });

  it("trojan horse with insufficient spam adds random damage cards", () => {
    const board = initEmptyBoard(10, 10);
    const robot = new Robot(1, 0, 0, Direction.N);
    const api = new BoardApiImpl(board, [robot]);

    const drawPile = [ProgramCard.left()];
    const deck = new Deck(drawPile, [], [], new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [robot]);
    game.setDeck(deck, 1);
    game.setDamageDecks(new DamageDecks(0, 15, 15));

    robot.loadProgram([ProgramCard.trojanHorse()]);
    game.executeOneRobotTurn(robot);

    const discardPile = game.getRobotDiscard(1);
    expect(discardPile.filter((c) => c.action === Action.SPAM).length).toBe(0);

    const trojanInDiscard = discardPile.filter(
      (c) => c.action === Action.TROJAN_HORSE,
    ).length;
    const wormInDiscard = discardPile.filter(
      (c) => c.action === Action.WORM,
    ).length;
    expect(trojanInDiscard + wormInDiscard).toBe(2);

    expect(game.getDamageDecks().getSpamDrawPile()).toBe(0);

    const totalDamageAfter =
      game.getDamageDecks().getTrojanHorseDrawPile() +
      game.getDamageDecks().getWormDrawPile();
    expect(totalDamageAfter).toBe(29);

    const trojanPile = game.getDamageDecks().getTrojanHorseDrawPile();
    expect(trojanPile >= 14 && trojanPile <= 16).toBe(true);
  });

  it("spam card executed then robot dies, spam removed and penalty applied", () => {
    const board = initEmptyBoard(3, 3);
    const r = new Robot(1, 2, 1, Direction.E);
    const api = new BoardApiImpl(board, [r]);

    const drawPile = [ProgramCard.move1()];
    const hand = [
      ProgramCard.spam(),
      ProgramCard.left(),
      ProgramCard.right(),
      ProgramCard.move2(),
      ProgramCard.uturn(),
    ];
    const deck = new Deck(drawPile, [], hand, new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);

    r.loadProgram([
      ProgramCard.spam(),
      ProgramCard.left(),
      ProgramCard.right(),
      ProgramCard.move2(),
      ProgramCard.uturn(),
    ]);

    game.executeOneRobotTurn(r);

    expect(r.isAlive()).toBe(false);
    expect(r.getX()).toBe(3);
    expect(r.getY()).toBe(1);
    expect(game.getRobotDiscard(1).some((c) => c.action === Action.MOVE)).toBe(true);
    expect(game.getRobotDiscard(1).filter((c) => c.action === Action.SPAM).length).toBe(2);
    expect(game.getDamageDecks().getSpamDrawPile()).toBe(37);
  });

  // Extension for issue #8: WORM reboot semantics, DamageDecks conservation
  // across dealNewHands, and the anti-cheat (#14) interplay. Ported 1:1 from
  // the reboot/penalty/putBack logic in Game.executeOneRobotTurn (Java lines
  // 632-642 of dk.dtu.domain.core.Game): applying the reboot penalty BEFORE
  // marking the robot dead, then clearing registers, removing WORM from hand,
  // and returning it to the pool. Note the WORM branch never touches robot
  // position: that only happens later, in applyRespawnPhase (see
  // gameReboot.test.ts / host/respawn.test.ts).
  it("worm card kills the robot, applies the reboot penalty, and returns to the pool", () => {
    const board = initEmptyBoard(5, 5);
    const r = new Robot(1, 2, 2, Direction.E);
    const api = new BoardApiImpl(board, [r]);

    const hand = [
      ProgramCard.worm(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ];
    const deck = new Deck([], [], hand, new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);

    r.loadProgram([ProgramCard.worm()]);
    game.executeOneRobotTurn(r);

    // Dead, registers cleared, position untouched (WORM never moves the robot).
    expect(r.isAlive()).toBe(false);
    expect(r.getX()).toBe(2);
    expect(r.getY()).toBe(2);
    expect(r.getRemainingProgram()).toEqual([]);

    // Reboot penalty drew 2 cards into discard (spam has priority: pool full).
    const discard = game.getRobotDiscard(1);
    expect(discard.filter((c) => c.action === Action.SPAM).length).toBe(2);

    // The WORM itself left the hand and went back to the pool, not discard.
    expect(game.getRobotHand(1).some((c) => c.action === Action.WORM)).toBe(false);
    expect(discard.some((c) => c.action === Action.WORM)).toBe(false);
    expect(game.getDamageDecks().getWormDrawPile()).toBe(16);
    // The 2 penalty draws came out of the spam pool.
    expect(game.getDamageDecks().getSpamDrawPile()).toBe(36);
  });

  it("worm card on an already-dead robot skips the reboot penalty but still returns to the pool", () => {
    const board = initEmptyBoard(5, 5);
    const r = new Robot(1, 1, 1, Direction.E);
    const api = new BoardApiImpl(board, [r]);
    r.setDead();

    const deck = new Deck([], [], [ProgramCard.worm()], new DamageDecks(38, 15, 15));
    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);

    r.loadProgram([ProgramCard.worm()]);
    game.executeOneRobotTurn(r);

    expect(r.isAlive()).toBe(false);
    // No penalty cards: robot was already dead when WORM resolved.
    expect(game.getRobotDiscard(1)).toEqual([]);
    expect(game.getDamageDecks().getWormDrawPile()).toBe(16);
    expect(game.getDamageDecks().getSpamDrawPile()).toBe(38);
  });

  it("damage cards in hand survive two consecutive dealNewHands (multi-round persistence)", () => {
    const board = initEmptyBoard(3, 3);
    const r = new Robot(1, 1, 1, Direction.E);
    const api = new BoardApiImpl(board, [r]);

    const drawPile: ProgramCard[] = [];
    for (let i = 0; i < 20; i++) drawPile.push(ProgramCard.move1());
    const hand = [ProgramCard.spam(), ProgramCard.move1(), ProgramCard.move1()];
    const deck = new Deck(drawPile, [], hand, new DamageDecks(38, 15, 15));

    const game = new Game(board, api, [r]);
    game.setDeck(deck, 1);

    game.dealNewHands();
    const roundOneHand = game.getRobotHand(1);
    expect(roundOneHand.filter((c) => c.action === Action.SPAM).length).toBe(1);
    expect(roundOneHand.length).toBe(9);

    // A second round's dealNewHands (SPAM was never played, so it is still in
    // hand and must be kept again, not re-drawn or duplicated).
    game.dealNewHands();
    const roundTwoHand = game.getRobotHand(1);
    expect(roundTwoHand.filter((c) => c.action === Action.SPAM).length).toBe(1);
    expect(roundTwoHand.length).toBe(9);
  });
});
