import { Phase } from "../core/phase.js";
import { Direction } from "../model/direction.js";
import type { Robot } from "../model/robot.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/**
 * Ported from dk.dtu.domain.rules.effects.Antenna.
 * Priority is Manhattan distance from the antenna, ties broken by clockwise
 * angle from the antenna's facing direction.
 */
export class Antenna implements TileEffect {
  constructor(readonly direction: Direction) {}

  onPhase(phase: Phase, tile: Tile, api: BoardAPI): void {
    if (phase !== Phase.ACTIVATE_ANTENNA) return;
    const x = tile.getX();
    const y = tile.getY();

    // Array.sort is stable, matching Java's stable sorted() over getRobots().
    const ordered = [...api.getRobots()].sort((a, b) => {
      const md =
        Antenna.manhattan(a.getX(), x, a.getY(), y) -
        Antenna.manhattan(b.getX(), x, b.getY(), y);
      if (md !== 0) return md;
      return (
        Antenna.tieBreaker(a, x, y, this.direction) -
        Antenna.tieBreaker(b, x, y, this.direction)
      );
    });

    api.updatePriorityList(ordered.map((r) => r.getId()));
  }

  private static manhattan(x1: number, x2: number, y1: number, y2: number): number {
    return Math.abs(x2 - x1) + Math.abs(y2 - y1);
  }

  private static tieBreaker(
    robot: Robot,
    antennaX: number,
    antennaY: number,
    antennaDir: Direction,
  ): number {
    const dx = robot.getX() - antennaX;
    const dy = robot.getY() - antennaY;

    let angleFromNorth = (Math.atan2(dx, -dy) * 180) / Math.PI;
    if (angleFromNorth < 0) angleFromNorth += 360;

    let startAngle: number;
    switch (antennaDir) {
      case Direction.N:
        startAngle = 0;
        break;
      case Direction.E:
        startAngle = 90;
        break;
      case Direction.S:
        startAngle = 180;
        break;
      case Direction.W:
        startAngle = 270;
        break;
    }

    let relativeAngle = angleFromNorth - startAngle;
    if (relativeAngle < 0) relativeAngle += 360;

    return Math.trunc(relativeAngle);
  }

  phases(): Set<Phase> | null {
    return new Set([Phase.ACTIVATE_ANTENNA]);
  }
}
