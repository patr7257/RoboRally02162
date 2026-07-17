import { Phase } from "../core/phase.js";
import type { Direction } from "../model/direction.js";
import type { Tile } from "../model/tile.js";
import type { BoardAPI } from "../rules/boardApi.js";
import type { TileEffect } from "./tileEffect.js";

/** Ported from dk.dtu.domain.rules.effects.Walls. */
export class Walls implements TileEffect {
  readonly edges: Set<Direction>;

  constructor(edges: Iterable<Direction>) {
    this.edges = new Set(edges);
  }

  static hasWall(tile: Tile | null, edge: Direction): boolean {
    if (tile === null) return false;
    for (const effect of tile.getEffects()) {
      if (effect instanceof Walls && effect.edges.has(edge)) {
        return true;
      }
    }
    return false;
  }

  getEdges(): Set<Direction> {
    return this.edges;
  }

  onPhase(_phase: Phase, _tile: Tile, _api: BoardAPI): void {
    // Walls are pure geometry; they have no phase behaviour.
  }

  phases(): Set<Phase> | null {
    return null;
  }
}
