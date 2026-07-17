import { Direction, turnRight, turnLeft } from "../model/direction.js";

/**
 * Ported from dk.dtu.domain.program.ProgramOP (sealed interface).
 * Each op has an apply(direction) that defaults to identity.
 */
export abstract class ProgramOP {
  apply(d: Direction): Direction {
    return d;
  }
}

export class MoveOp extends ProgramOP {
  constructor(readonly steps: number) {
    super();
  }
}

export class RotateRightOp extends ProgramOP {
  override apply(d: Direction): Direction {
    return turnRight(d);
  }
}

export class RotateLeftOp extends ProgramOP {
  override apply(d: Direction): Direction {
    return turnLeft(d);
  }
}

export class UTurnOp extends ProgramOP {
  override apply(d: Direction): Direction {
    return turnRight(turnRight(d));
  }
}

export class SpamOp extends ProgramOP {}
export class TrojanHorseOp extends ProgramOP {}
export class WormOp extends ProgramOP {}
export class AgainOp extends ProgramOP {}

export type ReactionKind = "SANDBOX" | "WEASEL" | "SPEED";

export class ReactionOp extends ProgramOP {
  constructor(readonly kind: ReactionKind) {
    super();
  }
}
