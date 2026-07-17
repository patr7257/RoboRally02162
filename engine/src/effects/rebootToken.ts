import { Phase } from "../core/phase.js";
import type { Direction } from "../model/direction.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.RebootToken. */
export class RebootToken implements TileEffect {
  constructor(readonly direction: Direction) {}

  onPhase(_phase: Phase, _tile: Tile, _api: BoardAPI): void {
    // Respawn is driven by Game.applyRespawnPhase, not by a phase pass.
  }

  phases(): Set<Phase> | null {
    return null;
  }
}
