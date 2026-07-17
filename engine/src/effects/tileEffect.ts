import type { Phase } from "../core/phase.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";

/** Ported from dk.dtu.domain.rules.effects.TileEffect. */
export interface TileEffect {
  onPhase(phase: Phase, tile: Tile, api: BoardAPI): void;
  /** Phases this effect triggers on, or null when it has no phase behaviour. */
  phases(): Set<Phase> | null;
}

export function triggersOn(effect: TileEffect, p: Phase): boolean {
  const phases = effect.phases();
  return phases !== null && phases.has(p);
}
