/** Ported from dk.dtu.domain.rules.Coord and Edge. */
export class Coord {
  constructor(
    readonly x: number,
    readonly y: number,
  ) {}

  isAdjacentTo(other: Coord): boolean {
    const dx = Math.abs(this.x - other.x);
    const dy = Math.abs(this.y - other.y);
    return dx + dy === 1;
  }

  equals(other: Coord): boolean {
    return this.x === other.x && this.y === other.y;
  }

  toString(): string {
    return `(${this.x}, ${this.y})`;
  }
}

export class Edge {
  constructor(
    readonly from: Coord,
    readonly to: Coord,
  ) {
    if (!from.isAdjacentTo(to)) {
      throw new Error(`Edge endpoints must be adjacent: ${from} -> ${to}`);
    }
  }

  equals(other: Edge): boolean {
    return this.from.equals(other.from) && this.to.equals(other.to);
  }
}
