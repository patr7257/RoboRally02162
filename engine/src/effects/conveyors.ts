import { Phase } from "../core/phase.js";
import type { Direction } from "../model/direction.js";
import type { Rotation } from "../model/rotation.js";
import type { Tile } from "../model/tile.js";
import { BeltIntent } from "../rules/outcome.js";
import { Coord } from "../rules/coord.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.GreenConveyor (speed 1, priority 1). */
export class GreenConveyor implements TileEffect {
  constructor(
    readonly direction: Direction,
    readonly rotation: Rotation,
  ) {}

  onPhase(phase: Phase, tile: Tile, api: BoardAPI): void {
    if (phase !== Phase.ACTIVATE_GREENCONVEYOR) return;
    for (const r of api.getRobotsOnTile(tile.getX(), tile.getY())) {
      if (!r.isAlive() || r.movedOnActivation()) continue;
      const from = new Coord(r.getX(), r.getY());
      const to = api.next(from, this.direction);
      api.addIntent(new BeltIntent(r.getId(), from, to, 1, 1, this.rotation));
    }
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_GREENCONVEYOR]);
  }
}

/** Ported from dk.dtu.domain.rules.effects.BlueConveyor (speed 2, priority 2). */
export class BlueConveyor implements TileEffect {
  constructor(
    readonly direction: Direction,
    readonly rotation: Rotation,
  ) {}

  onPhase(phase: Phase, tile: Tile, api: BoardAPI): void {
    if (phase !== Phase.ACTIVATE_BLUECONVEYOR) return;
    for (const r of api.getRobotsOnTile(tile.getX(), tile.getY())) {
      if (!r.isAlive() || r.movedOnActivation()) continue;
      const from = new Coord(r.getX(), r.getY());
      const to = api.next(from, this.direction);
      api.addIntent(new BeltIntent(r.getId(), from, to, 2, 2, this.rotation));
    }
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_BLUECONVEYOR]);
  }
}
