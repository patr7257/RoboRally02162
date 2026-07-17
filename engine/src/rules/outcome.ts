import { Coord, Edge } from "./coord.js";
import type { Rotation } from "../model/rotation.js";

/** Ported from dk.dtu.domain.rules.DestroyCause. */
export const DestroyCause = {
  FELL_OFF: "FELL_OFF",
  PITS: "PITS",
  LASER: "LASER",
} as const;

export type DestroyCause = (typeof DestroyCause)[keyof typeof DestroyCause];

/** Ported from dk.dtu.domain.rules.MoveEvent. */
export class MoveEvent {
  constructor(
    readonly robotId: number,
    readonly from: Coord,
    readonly to: Coord,
  ) {}
}

/** Ported from dk.dtu.domain.rules.DestroyEvent (power defaults to 0). */
export class DestroyEvent {
  constructor(
    readonly robotId: number,
    readonly at: Coord,
    readonly cause: DestroyCause,
    readonly power: number = 0,
  ) {}
}

/** Ported from dk.dtu.domain.rules.EdgeBlock (BlockReason + StopReason). */
export class EdgeBlock {
  constructor(readonly edge: Edge) {}
}

/** Ported from dk.dtu.domain.rules.RobotChainImmovable. */
export class RobotChainImmovable {
  readonly chain: number[];
  constructor(
    chain: number[],
    readonly stop: EdgeBlock,
  ) {
    if (chain.length === 0) throw new Error("chain must be non-empty");
    this.chain = [...chain];
  }
}

/** Java sealed BlockReason permits EdgeBlock, RobotChainImmovable. */
export type BlockReason = EdgeBlock | RobotChainImmovable;

/** Ported from dk.dtu.domain.rules.Outcome (sealed Moved | Blocked). */
export class Moved {
  constructor(
    readonly moves: MoveEvent[],
    readonly destroys: DestroyEvent[],
  ) {}
}

export class Blocked {
  constructor(readonly reason: BlockReason) {}
}

export type Outcome = Moved | Blocked;

/** Ported from dk.dtu.domain.rules.BeltIntent. */
export class BeltIntent {
  constructor(
    readonly robotId: number,
    readonly from: Coord,
    readonly to: Coord,
    readonly speed: number,
    readonly priority: number,
    readonly rotateAfter: Rotation,
  ) {}
}
