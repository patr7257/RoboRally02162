import { Phase } from "../core/phase.js";
import type { Tile } from "../model/tile.js";
import { Coord } from "../rules/coord.js";
import { DestroyCause } from "../rules/outcome.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.Pits. */
export class Pits implements TileEffect {
  onPhase(_phase: Phase, tile: Tile, api: BoardAPI): void {
    const x = tile.getX();
    const y = tile.getY();
    for (const robot of api.getRobotsOnTile(x, y)) {
      api.reportDestroy(robot.getId(), new Coord(x, y), DestroyCause.PITS);
    }
  }

  static hasPits(tile: Tile | null): boolean {
    if (tile === null) return false;
    for (const effect of tile.getEffects()) {
      if (effect instanceof Pits) return true;
    }
    return false;
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_PITS]);
  }
}
