import { Phase } from "../core/phase.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.Checkpoint. */
export class Checkpoint implements TileEffect {
  constructor(readonly number: number) {}

  onPhase(_phase: Phase, tile: Tile, api: BoardAPI): void {
    const x = tile.getX();
    const y = tile.getY();
    for (const robot of api.getRobotsOnTile(x, y)) {
      robot.advanceCheckpointIfMatches(this.number);
    }
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_CHECKPOINTS]);
  }
}
