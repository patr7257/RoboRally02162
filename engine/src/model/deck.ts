import { ProgramCard } from "../program/programCard.js";
import { SpamOp, TrojanHorseOp, WormOp } from "../program/programOp.js";
import { DamageDecks } from "./damageDecks.js";

function cardEquals(a: ProgramCard, b: ProgramCard): boolean {
  return a.action === b.action && a.steps === b.steps;
}

/** Removes the first element equal to card; returns true if one was removed. */
function removeFirst(list: ProgramCard[], card: ProgramCard): boolean {
  const i = list.findIndex((c) => cardEquals(c, card));
  if (i >= 0) {
    list.splice(i, 1);
    return true;
  }
  return false;
}

function shuffle<T>(list: T[]): void {
  for (let i = list.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [list[i], list[j]] = [list[j], list[i]];
  }
}

/**
 * Ported from dk.dtu.domain.model.Deck.
 * drawPile is front-to-back: index 0 is the top of the pile.
 */
export class Deck {
  private drawPile: ProgramCard[];
  private discardPile: ProgramCard[];
  private hand: ProgramCard[];
  private readonly dDecks: DamageDecks;

  constructor(
    drawPile: ProgramCard[],
    discardPile: ProgramCard[],
    hand: ProgramCard[],
    dDecks: DamageDecks,
  ) {
    this.drawPile = drawPile;
    this.discardPile = discardPile;
    this.hand = hand;
    this.dDecks = dDecks;
  }

  /** Equivalent to Java's Deck(DamageDecks) constructor with a shuffled standard deck. */
  static standard(dDecks: DamageDecks): Deck {
    return new Deck(Deck.buildStandardDeck(), [], [], dDecks);
  }

  draw(): void {
    if (this.drawPile.length === 0) this.reshuffle();
    if (this.drawPile.length === 0) return;
    const drawCard = this.drawPile.shift()!;
    this.hand.push(drawCard);
  }

  popTop(): ProgramCard {
    if (this.drawPile.length === 0) this.reshuffle();
    if (this.drawPile.length === 0) {
      throw new Error("No cards to pop from draw pile");
    }
    return this.drawPile.shift()!;
  }

  discardTopCard(): void {
    if (this.drawPile.length === 0) this.reshuffle();
    if (this.drawPile.length === 0) return;
    const card = this.popTop();
    this.discard(card);
  }

  peekDrawPileTop(): ProgramCard | null {
    if (this.drawPile.length === 0) this.reshuffle();
    if (this.drawPile.length === 0) return null;
    return this.drawPile[0];
  }

  dealHand(count: number): void {
    const damageInHand = this.discardHand();
    for (let i = 0; i < count - damageInHand; i++) {
      this.draw();
    }
  }

  discardHand(): number {
    const damageCards: ProgramCard[] = [];
    const regularCards: ProgramCard[] = [];

    for (const card of this.hand) {
      if (this.isDamageCard(card)) damageCards.push(card);
      else regularCards.push(card);
    }

    for (const card of regularCards) this.discard(card);

    this.hand = [...damageCards];
    return damageCards.length;
  }

  discard(card: ProgramCard): void {
    if (this.isDamageCard(card)) {
      this.dDecks.putBack(card);
    } else {
      this.discardPile.push(card);
    }
  }

  addToDiscard(card: ProgramCard): void {
    this.discardPile.push(card);
  }

  isDamageCard(card: ProgramCard): boolean {
    const op = card.toOp();
    return (
      op instanceof SpamOp ||
      op instanceof TrojanHorseOp ||
      op instanceof WormOp
    );
  }

  validateAndCompleteOrThrow(picked: ProgramCard[]): ProgramCard[] {
    if (picked == null) throw new Error("cards null");
    if (picked.length > 5) throw new Error("Play at most 5 cards");

    const tempHand = [...this.hand];
    for (const c of picked) {
      if (!removeFirst(tempHand, c)) {
        throw new Error("Not enough " + c + " in hand");
      }
    }

    const result: ProgramCard[] = [...picked];
    while (result.length < 5) {
      const extra = this.drawForProgramAutocomplete();
      if (extra === null) {
        throw new Error("Unable to complete to 5 with current deck");
      }
      result.push(extra);
    }
    return result;
  }

  private drawForProgramAutocomplete(): ProgramCard | null {
    if (this.drawPile.length === 0) this.reshuffle();
    if (this.drawPile.length === 0) return null;
    const c = this.drawPile.shift()!;
    this.hand.push(c);
    return c;
  }

  private reshuffle(): void {
    if (this.discardPile.length === 0) return;
    shuffle(this.discardPile);
    this.drawPile.push(...this.discardPile);
    this.discardPile = [];
  }

  static buildStandardDeck(): ProgramCard[] {
    const cards: ProgramCard[] = [];
    const add = (supplier: () => ProgramCard, n: number) => {
      for (let i = 0; i < n; i++) cards.push(supplier());
    };
    add(ProgramCard.move1, 4);
    add(ProgramCard.move2, 3);
    add(ProgramCard.move3, 1);
    add(ProgramCard.back1, 1);
    add(ProgramCard.again, 1);
    add(ProgramCard.left, 4);
    add(ProgramCard.right, 4);
    add(ProgramCard.uturn, 1);
    add(ProgramCard.sandbox, 1);
    add(ProgramCard.weasel, 1);
    add(ProgramCard.speed, 1);
    shuffle(cards);
    return cards;
  }

  getHand(): ProgramCard[] {
    return [...this.hand];
  }

  getDrawPile(): ProgramCard[] {
    return [...this.drawPile];
  }

  getDiscardPile(): ProgramCard[] {
    return [...this.discardPile];
  }

  removeFromHand(card: ProgramCard): void {
    removeFirst(this.hand, card);
  }

  acceptCardsAsIs(cards: ProgramCard[]): ProgramCard[] {
    if (cards == null) throw new Error("cards null");
    if (cards.length > 5) return cards.slice(0, 5);
    return [...cards];
  }
}
