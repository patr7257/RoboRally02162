import { describe, it, expect } from "vitest";
import { DamageDecks } from "../src/model/damageDecks.js";
import { ProgramCard } from "../src/program/programCard.js";
import { Action } from "../src/program/programCard.js";

/**
 * Ported from dk.dtu.domain.model.DamageDecks (drawDamageCards/drawOne/putBack).
 * Extension for issue #8: exercises the pool class directly, independent of
 * Game, since gameDamageCard.test.ts already covers it through the robot
 * turn resolution loop.
 */
describe("DamageDecks", () => {
  it("draws SPAM first while the spam pool has cards, regardless of trojan/worm counts", () => {
    const decks = new DamageDecks(3, 15, 15);
    const drawn = decks.drawDamageCards(3);
    expect(drawn.every((c) => c.action === Action.SPAM)).toBe(true);
    expect(decks.getSpamDrawPile()).toBe(0);
    expect(decks.getTrojanHorseDrawPile()).toBe(15);
    expect(decks.getWormDrawPile()).toBe(15);
  });

  it("falls back to trojan/worm at random once spam is exhausted", () => {
    const decks = new DamageDecks(0, 15, 15);
    const drawn = decks.drawDamageCards(30);
    expect(drawn).toHaveLength(30);
    expect(drawn.every((c) => c.action === Action.TROJAN_HORSE || c.action === Action.WORM)).toBe(
      true,
    );
    expect(decks.getSpamDrawPile()).toBe(0);
    expect(decks.getTrojanHorseDrawPile()).toBe(0);
    expect(decks.getWormDrawPile()).toBe(0);
  });

  it("stops early (returns fewer than requested) once every pool is exhausted", () => {
    const decks = new DamageDecks(0, 0, 0);
    const drawn = decks.drawDamageCards(5);
    expect(drawn).toEqual([]);
    expect(decks.getSpamDrawPile()).toBe(0);
    expect(decks.getTrojanHorseDrawPile()).toBe(0);
    expect(decks.getWormDrawPile()).toBe(0);
  });

  it("stops mid-request once the last card is drawn (partial exhaustion)", () => {
    const decks = new DamageDecks(0, 1, 0);
    const drawn = decks.drawDamageCards(5);
    expect(drawn).toHaveLength(1);
    expect(drawn[0].action).toBe(Action.TROJAN_HORSE);
    expect(decks.getTrojanHorseDrawPile()).toBe(0);
  });

  it("putBack restores exactly the pool matching the card's action", () => {
    const decks = new DamageDecks(0, 0, 0);
    decks.putBack(ProgramCard.spam());
    decks.putBack(ProgramCard.trojanHorse());
    decks.putBack(ProgramCard.worm());
    decks.putBack(ProgramCard.worm());

    expect(decks.getSpamDrawPile()).toBe(1);
    expect(decks.getTrojanHorseDrawPile()).toBe(1);
    expect(decks.getWormDrawPile()).toBe(2);
  });

  it("conserves the total pool size across a draw/putBack round trip", () => {
    const decks = new DamageDecks(38, 15, 15);
    const total = () =>
      decks.getSpamDrawPile() + decks.getTrojanHorseDrawPile() + decks.getWormDrawPile();
    expect(total()).toBe(68);

    const drawn = decks.drawDamageCards(20);
    expect(total()).toBe(48);

    for (const c of drawn) decks.putBack(c);
    expect(total()).toBe(68);
    expect(decks.getSpamDrawPile()).toBe(38);
    expect(decks.getTrojanHorseDrawPile()).toBe(15);
    expect(decks.getWormDrawPile()).toBe(15);
  });
});
