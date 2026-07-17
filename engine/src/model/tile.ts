import { Phase } from "../core/phase.js";
import type { TileEffect } from "../effects/tileEffect.js";

/** Ported from dk.dtu.domain.model.Tile. */
export class Tile {
  x: number;
  y: number;
  private effects: TileEffect[];

  constructor(x: number, y: number, effects: TileEffect[] = []) {
    this.x = x;
    this.y = y;
    this.effects = effects;
  }

  setEffects(effects: TileEffect[]): void {
    this.effects = effects;
  }

  addEffect(effect: TileEffect): void {
    this.effects.push(effect);
  }

  removeEffect(effect: TileEffect): void {
    const i = this.effects.indexOf(effect);
    if (i >= 0) this.effects.splice(i, 1);
  }

  getX(): number {
    return this.x;
  }

  getY(): number {
    return this.y;
  }

  getEffects(): TileEffect[] {
    return this.effects;
  }

  getEffectsForPhase(phase: Phase): TileEffect[] {
    const out: TileEffect[] = [];
    for (const e of this.effects) {
      const phases = e.phases();
      if (phases !== null && phases.has(phase)) {
        out.push(e);
      }
    }
    return out;
  }
}
