import { Phase } from "../core/phase.js";
import { Rotation } from "../model/rotation.js";
import { turnLeft, turnRight } from "../model/direction.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.Gear. */
export class Gear implements TileEffect {
  constructor(readonly rotation: Rotation) {}

  onPhase(_phase: Phase, tile: Tile, api: BoardAPI): void {
    const robotsOnTile = api.getRobotsOnTile(tile.getX(), tile.getY());
    if (robotsOnTile.length === 0) return;
    for (const robot of robotsOnTile) {
      switch (this.rotation) {
        case Rotation.LEFT:
          robot.setDirection(turnLeft(robot.getDirection()));
          break;
        case Rotation.RIGHT:
          robot.setDirection(turnRight(robot.getDirection()));
          break;
        case Rotation.NONE:
          return;
      }
    }
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_GEAR]);
  }
}
