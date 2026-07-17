/**
 * Ported from dk.dtu.domain.model.Direction.
 * Enum order N, E, S, W (ordinals 0..3) drives all turn math.
 */
export const Direction = {
  N: "N",
  E: "E",
  S: "S",
  W: "W",
} as const;

export type Direction = (typeof Direction)[keyof typeof Direction];

const ORDER: Direction[] = [Direction.N, Direction.E, Direction.S, Direction.W];

export function ordinal(d: Direction): number {
  return ORDER.indexOf(d);
}

export function turnRight(d: Direction): Direction {
  return ORDER[(ordinal(d) + 1) % ORDER.length];
}

export function turnLeft(d: Direction): Direction {
  return ORDER[(ordinal(d) + ORDER.length - 1) % ORDER.length];
}

export function opposite(d: Direction): Direction {
  return ORDER[(ordinal(d) + 2) % ORDER.length];
}

/**
 * Maps a coordinate delta to a heading, or null when the two points are not
 * orthogonal neighbours. Mirrors Direction.fromDelta.
 */
export function fromDelta(
  x1: number,
  y1: number,
  x2: number,
  y2: number,
): Direction | null {
  if (x1 === x2) {
    if (y2 > y1) return Direction.S;
    if (y2 < y1) return Direction.N;
  } else if (y1 === y2) {
    if (x2 > x1) return Direction.E;
    if (x2 < x1) return Direction.W;
  }
  return null;
}
