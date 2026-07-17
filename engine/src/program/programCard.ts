import {
  ProgramOP,
  MoveOp,
  RotateRightOp,
  RotateLeftOp,
  UTurnOp,
  SpamOp,
  TrojanHorseOp,
  WormOp,
  AgainOp,
  ReactionOp,
} from "./programOp.js";

/** Ported from dk.dtu.domain.program.ProgramCard.Action. */
export const Action = {
  MOVE: "MOVE",
  ROTATERIGHT: "ROTATERIGHT",
  ROTATELEFT: "ROTATELEFT",
  UTURN: "UTURN",
  SPAM: "SPAM",
  TROJAN_HORSE: "TROJAN_HORSE",
  WORM: "WORM",
  SANDBOX: "SANDBOX",
  WEASEL: "WEASEL",
  SPEED: "SPEED",
  AGAIN: "AGAIN",
} as const;

export type Action = (typeof Action)[keyof typeof Action];

/**
 * Ported from dk.dtu.domain.program.ProgramCard.
 * MOVE steps must be in -1..3; every non-MOVE action forces steps to 0.
 */
export class ProgramCard {
  readonly action: Action;
  readonly steps: number;

  constructor(action: Action, steps: number) {
    this.action = action;
    if (action === Action.MOVE) {
      if (steps < -1 || steps > 3) {
        throw new Error("MOVE step must be -1..3");
      }
      this.steps = steps;
    } else {
      this.steps = 0;
    }
  }

  toOps(): ProgramOP[] {
    return [this.toOp()];
  }

  toOp(): ProgramOP {
    switch (this.action) {
      case Action.MOVE:
        return new MoveOp(this.steps);
      case Action.ROTATERIGHT:
        return new RotateRightOp();
      case Action.ROTATELEFT:
        return new RotateLeftOp();
      case Action.UTURN:
        return new UTurnOp();
      case Action.SPAM:
        return new SpamOp();
      case Action.TROJAN_HORSE:
        return new TrojanHorseOp();
      case Action.WORM:
        return new WormOp();
      case Action.SANDBOX:
        return new ReactionOp("SANDBOX");
      case Action.WEASEL:
        return new ReactionOp("WEASEL");
      case Action.SPEED:
        return new ReactionOp("SPEED");
      case Action.AGAIN:
        return new AgainOp();
    }
  }

  toString(): string {
    if (this.action === Action.MOVE) return "MOVE" + this.steps;
    return this.action;
  }

  static move1(): ProgramCard {
    return new ProgramCard(Action.MOVE, 1);
  }
  static move2(): ProgramCard {
    return new ProgramCard(Action.MOVE, 2);
  }
  static move3(): ProgramCard {
    return new ProgramCard(Action.MOVE, 3);
  }
  static back1(): ProgramCard {
    return new ProgramCard(Action.MOVE, -1);
  }
  static right(): ProgramCard {
    return new ProgramCard(Action.ROTATERIGHT, 0);
  }
  static left(): ProgramCard {
    return new ProgramCard(Action.ROTATELEFT, 0);
  }
  static uturn(): ProgramCard {
    return new ProgramCard(Action.UTURN, 0);
  }
  static spam(): ProgramCard {
    return new ProgramCard(Action.SPAM, 0);
  }
  static trojanHorse(): ProgramCard {
    return new ProgramCard(Action.TROJAN_HORSE, 0);
  }
  static worm(): ProgramCard {
    return new ProgramCard(Action.WORM, 0);
  }
  static sandbox(): ProgramCard {
    return new ProgramCard(Action.SANDBOX, 0);
  }
  static weasel(): ProgramCard {
    return new ProgramCard(Action.WEASEL, 0);
  }
  static speed(): ProgramCard {
    return new ProgramCard(Action.SPEED, 0);
  }
  static again(): ProgramCard {
    return new ProgramCard(Action.AGAIN, 0);
  }
}
