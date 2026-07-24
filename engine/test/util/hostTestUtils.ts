import { Board } from "../../src/model/board.js";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { Checkpoint } from "../../src/effects/checkpoint.js";
import { Pits } from "../../src/effects/pits.js";
import { RebootToken } from "../../src/effects/rebootToken.js";
import {
  BoardSnapshot,
  CardSnapshot,
  GameSnapshot,
  boardToSnapshot,
  cardToSnapshot,
} from "../../src/host/snapshot.js";
import { initEmptyCells } from "./boardTestUtils.js";

/** Card list -> snapshot list, the shape the host API takes. */
export const prog = (...cards: ProgramCard[]): CardSnapshot[] =>
  cards.map(cardToSnapshot);

/**
 * A board whose single checkpoint sits in the far corner. A board with zero
 * checkpoints would declare a winner after the first register (hasWon(0) is
 * always true), which would cut every test round short.
 */
export function boardWithFarCheckpoint(
  width: number,
  height: number,
): BoardSnapshot {
  const tiles = initEmptyCells(width, height);
  tiles[width - 1][height - 1].setEffects([new Checkpoint(1)]);
  return boardToSnapshot(new Board(width, height, tiles));
}

/** Far checkpoint, a pit at (0,1) and a reboot token at (2,2) facing east. */
export function boardWithPitAndRebootToken(
  width: number,
  height: number,
): BoardSnapshot {
  const tiles = initEmptyCells(width, height);
  tiles[width - 1][height - 1].setEffects([new Checkpoint(1)]);
  tiles[0][1].setEffects([new Pits()]);
  tiles[2][2].setEffects([new RebootToken(Direction.E)]);
  return boardToSnapshot(new Board(width, height, tiles));
}

function clone(snapshot: GameSnapshot): GameSnapshot {
  return JSON.parse(JSON.stringify(snapshot)) as GameSnapshot;
}

/** Replaces a robot's hand, so a submitted pick is deterministic. */
export function withHand(
  snapshot: GameSnapshot,
  robotId: number,
  cards: ProgramCard[],
): GameSnapshot {
  const next = clone(snapshot);
  next.decks[String(robotId)].hand = cards.map(cardToSnapshot);
  return next;
}

/** Replaces a robot's draw pile, so flips and auto-completes are deterministic. */
export function withDrawPile(
  snapshot: GameSnapshot,
  robotId: number,
  cards: ProgramCard[],
): GameSnapshot {
  const next = clone(snapshot);
  next.decks[String(robotId)].drawPile = cards.map(cardToSnapshot);
  return next;
}

/** Replaces a robot's discard pile. */
export function withDiscardPile(
  snapshot: GameSnapshot,
  robotId: number,
  cards: ProgramCard[],
): GameSnapshot {
  const next = clone(snapshot);
  next.decks[String(robotId)].discardPile = cards.map(cardToSnapshot);
  return next;
}

/**
 * Locks a program straight into the snapshot, bypassing hand validation and the
 * auto-complete draw. For tests about what the engine does with a given
 * program, not about submitProgram itself.
 */
export function withProgram(
  snapshot: GameSnapshot,
  robotId: number,
  cards: ProgramCard[],
): GameSnapshot {
  const next = clone(snapshot);
  const player = next.players.find((p) => p.robotId === robotId);
  if (!player) throw new Error("No player for robot " + robotId);
  player.program = cards.map(cardToSnapshot);
  player.locked = true;
  return next;
}

/** JSON round-trip, to prove a paused snapshot is fully serializable. */
export function throughJson(snapshot: GameSnapshot): GameSnapshot {
  return JSON.parse(JSON.stringify(snapshot)) as GameSnapshot;
}
