import {
  MoveOp,
  ProgramOP,
  RotateLeftOp,
  RotateRightOp,
  UTurnOp,
} from "./programOp.js";
import type { ReactionKind } from "./programOp.js";

/**
 * Reaction (interactive card) specs, ported from
 * dk.dtu.domain.core.reaction.ReactionChoice plus
 * GameScheduler.createReactionSpec: SANDBOX offers every concrete op and
 * defaults to MOVE1, WEASEL offers the three rotations and defaults to LEFT,
 * SPEED has the single option MOVE3.
 *
 * The chosen option REPLACES the reaction op with a concrete op, which is what
 * the robot then executes (and what a following AGAIN card repeats).
 */

export type ReactionChoice =
  | "MOVE1"
  | "MOVE2"
  | "MOVE3"
  | "BACKUP"
  | "LEFT"
  | "RIGHT"
  | "UTURN";

export interface ReactionSpec {
  kind: ReactionKind;
  options: ReactionChoice[];
  defaultChoice: ReactionChoice;
}

/** Oracle options and defaults, in Java enum declaration order. */
export const REACTION_SPECS: Record<ReactionKind, ReactionSpec> = {
  SANDBOX: {
    kind: "SANDBOX",
    options: ["MOVE1", "MOVE2", "MOVE3", "BACKUP", "LEFT", "RIGHT", "UTURN"],
    defaultChoice: "MOVE1",
  },
  WEASEL: {
    kind: "WEASEL",
    options: ["LEFT", "RIGHT", "UTURN"],
    defaultChoice: "LEFT",
  },
  SPEED: {
    kind: "SPEED",
    options: ["MOVE3"],
    defaultChoice: "MOVE3",
  },
};

/** Maps a resolved choice to the concrete op that replaces the reaction. */
export function choiceToOp(choice: ReactionChoice): ProgramOP {
  switch (choice) {
    case "MOVE1":
      return new MoveOp(1);
    case "MOVE2":
      return new MoveOp(2);
    case "MOVE3":
      return new MoveOp(3);
    case "BACKUP":
      return new MoveOp(-1);
    case "LEFT":
      return new RotateLeftOp();
    case "RIGHT":
      return new RotateRightOp();
    case "UTURN":
      return new UTurnOp();
  }
}

/**
 * Falls back to the spec default for a missing choice or one that is not legal
 * for this reaction kind (the Java scheduler applies the default on timeout).
 */
export function normalizeChoice(
  kind: ReactionKind,
  choice: ReactionChoice | null | undefined,
): ReactionChoice {
  const spec = REACTION_SPECS[kind];
  if (choice != null && spec.options.includes(choice)) return choice;
  return spec.defaultChoice;
}
