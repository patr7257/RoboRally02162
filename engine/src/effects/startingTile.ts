import { Phase } from "../core/phase.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.StartingTile. */
export class StartingTile implements TileEffect {
  constructor(readonly robotId: number) {}

  onPhase(_phase: Phase, _tile: Tile, _api: BoardAPI): void {
    // No phase behaviour; used only for initial placement metadata.
  }

  phases(): Set<Phase> | null {
    return new Set();
  }
}
