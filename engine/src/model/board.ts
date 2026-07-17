import { Tile } from "./tile.js";

/** Ported from dk.dtu.domain.model.Board. Tiles stored column-major: tiles[x][y]. */
export class Board {
  constructor(
    readonly width: number,
    readonly height: number,
    private readonly tiles: Tile[][],
  ) {}

  getWidth(): number {
    return this.width;
  }

  getHeight(): number {
    return this.height;
  }

  getCells(): Tile[][] {
    return this.tiles;
  }

  getTiles(): Tile[][] {
    return this.tiles;
  }

  isInBounds(x: number, y: number): boolean {
    return x >= 0 && y >= 0 && x < this.width && y < this.height;
  }

  getTile(x: number, y: number): Tile {
    return this.tiles[x][y];
  }
}
