import { Phase } from "../core/phase.js";
import type { Board } from "../model/board.js";
import type { Robot } from "../model/robot.js";
import type { Tile } from "../model/tile.js";
import { Coord } from "../rules/coord.js";
import { DestroyCause } from "../rules/outcome.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";
import { Antenna } from "./antenna.js";

/** Ported from dk.dtu.domain.rules.effects.RobotLaser (always 1 damage). */
export class RobotLaser implements TileEffect {
  constructor(readonly robot: Robot) {}

  onPhase(phase: Phase, tile: Tile, api: BoardAPI): void {
    if (phase !== Phase.ACTIVATE_ROBOT_LASERS) return;

    let hasTarget = false;

    let currentPos = new Coord(tile.getX(), tile.getY());
    let previousPos = currentPos;

    const direction = this.robot.getDirection();

    for (;;) {
      currentPos = api.next(currentPos, direction);

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
        if (target.getId() !== this.robot.getId()) {
          api.reportDestroy(target.getId(), currentPos, DestroyCause.LASER, 1);
          hasTarget = true;
        }
        break;
      }

      previousPos = currentPos;
    }

    if (hasTarget) {
      api.notifyTileEffectActivated(tile.getX(), tile.getY(), "robot_laser");
    }
  }

  /**
   * Ported from RobotLaser.applyRobotLaserEffects: dynamically add a laser for
   * every alive robot at its current tile, fire them, then remove them.
   */
  static applyRobotLaserEffects(
    phase: Phase,
    robots: Robot[],
    board: Board,
    api: BoardAPI,
  ): void {
    const addedEffects = new Map<Tile, TileEffect[]>();
    for (const robot of robots) {
      if (robot.isAlive()) {
        const tile = board.getTile(robot.getX(), robot.getY());
        if (tile) {
          const laserEffect = new RobotLaser(robot);
          tile.addEffect(laserEffect);
          const list = addedEffects.get(tile) ?? [];
          list.push(laserEffect);
          addedEffects.set(tile, list);
        }
      }
    }

    for (const tile of addedEffects.keys()) {
      for (const effect of tile.getEffectsForPhase(phase)) {
        if (effect instanceof RobotLaser) {
          effect.onPhase(phase, tile, api);
        }
      }
    }

    for (const [tile, effects] of addedEffects) {
      for (const effect of effects) {
        tile.removeEffect(effect);
      }
    }
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_ROBOT_LASERS]);
  }
}
