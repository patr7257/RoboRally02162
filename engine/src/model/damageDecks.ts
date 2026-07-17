import { ProgramCard } from "../program/programCard.js";
import { SpamOp, TrojanHorseOp, WormOp } from "../program/programOp.js";

/**
 * Ported from dk.dtu.domain.model.DamageDecks.
 * Global pools for SPAM / TROJAN_HORSE / WORM damage cards.
 */
export class DamageDecks {
  private spamDrawPile: number;
  private trojanHorseDrawPile: number;
  private wormDrawPile: number;

  constructor(spamCount: number, trojanHorseCount: number, wormCount: number) {
    this.spamDrawPile = spamCount;
    this.trojanHorseDrawPile = trojanHorseCount;
    this.wormDrawPile = wormCount;
  }

  drawDamageCards(count: number): ProgramCard[] {
    const result: ProgramCard[] = [];
    for (let i = 0; i < count; i++) {
      const c = this.drawOne();
      if (c === null) break;
      result.push(c);
    }
    return result;
  }

  private drawOne(): ProgramCard | null {
    if (this.spamDrawPile > 0) {
      this.spamDrawPile--;
      return ProgramCard.spam();
    }

    const options: ProgramCard[] = [];
    if (this.trojanHorseDrawPile > 0) options.push(ProgramCard.trojanHorse());
    if (this.wormDrawPile > 0) options.push(ProgramCard.worm());

    if (options.length === 0) return null;

    const selected = options[Math.floor(Math.random() * options.length)];
    const op = selected.toOp();
    if (op instanceof TrojanHorseOp) {
      this.trojanHorseDrawPile--;
    } else if (op instanceof WormOp) {
      this.wormDrawPile--;
    }
    return selected;
  }

  putBack(card: ProgramCard): void {
    const op = card.toOp();
    if (op instanceof SpamOp) this.spamDrawPile++;
    else if (op instanceof TrojanHorseOp) this.trojanHorseDrawPile++;
    else if (op instanceof WormOp) this.wormDrawPile++;
  }

  getSpamDrawPile(): number {
    return this.spamDrawPile;
  }

  getTrojanHorseDrawPile(): number {
    return this.trojanHorseDrawPile;
  }

  getWormDrawPile(): number {
    return this.wormDrawPile;
  }

  setSpamDrawPile(v: number): void {
    this.spamDrawPile = v;
  }

  setTrojanHorseDrawPile(v: number): void {
    this.trojanHorseDrawPile = v;
  }

  setWormDrawPile(v: number): void {
    this.wormDrawPile = v;
  }
}
