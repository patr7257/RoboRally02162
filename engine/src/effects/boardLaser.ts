import { Phase } from "../core/phase.js";
import type { Direction } from "../model/direction.js";
import type { Tile } from "../model/tile.js";
import { Coord } from "../rules/coord.js";
import { DestroyCause } from "../rules/outcome.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";
import { Antenna } from "./antenna.js";

/** Ported from dk.dtu.domain.rules.effects.BoardLaser. */
export class BoardLaser implements TileEffect {
  constructor(
    readonly direction: Direction,
    readonly power: number,
  ) {}

  onPhase(phase: Phase, tile: Tile, api: BoardAPI): void {
    if (phase !== Phase.ACTIVATE_BOARD_LASERS) return;

    let hasTarget = false;

    let currentPos = new Coord(tile.getX(), tile.getY());
    let previousPos = currentPos;

    // A robot standing on the laser tile is hit directly (nothing can block it).
    const robotsOnLaser = api.getRobotsOnTile(currentPos.x, currentPos.y);
    if (robotsOnLaser.length > 0) {
      const target = robotsOnLaser[0];
      api.reportDestroy(target.getId(), currentPos, DestroyCause.LASER, this.power);
      api.notifyTileEffectActivated(tile.getX(), tile.getY(), "board_laser");
      return;
    }

    for (;;) {
      currentPos = api.next(currentPos, this.direction);

      if (!api.isInBounds(currentPos.x, currentPos.y)) break;

      const currentTile = api.getTile(currentPos.x, currentPos.y);

      if (api.hasWallBetween(previousPos, currentPos)) break;

      let blockedByAntenna = false;
      if (currentTile) {
        for (const effect of currentTile.getEffects()) {
          if (effect instanceof Antenna) {
            blockedByAntenna = true;
            break;
          }
        }
      }
      if (blockedByAntenna) break;

      const robotsHere = api.getRobotsOnTile(currentPos.x, currentPos.y);
      if (robotsHere.length > 0) {
        const target = robotsHere[0];
        api.reportDestroy(target.getId(), currentPos, DestroyCause.LASER, this.power);
        hasTarget = true;
        break;
      }

      previousPos = currentPos;
    }

    if (hasTarget) {
      api.notifyTileEffectActivated(tile.getX(), tile.getY(), "board_laser");
    }
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_BOARD_LASERS]);
  }
}
