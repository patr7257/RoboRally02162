// Bridges the ported RoboRally engine (GameSnapshot / CardSnapshot, UPPER_SNAKE
// effect kinds, column-major tiles[x][y]) to the shapes the existing React game
// view consumes (GameData / Board / Robot / TileEffect and the MoveType card
// enum). Pure functions, no side effects.

import type {
  GameSnapshot,
  CardSnapshot,
  EffectSnapshot,
  Frame,
} from "../engine/roborally-engine";
import {
  MoveType,
  GameData,
  Board,
  Tile,
  Robot,
  TileEffect,
} from "../types/boardTypes";

// ---- cards ---------------------------------------------------------------

/** Client card enum -> engine {action, steps}. */
export function moveTypeToCard(m: MoveType): CardSnapshot {
  switch (m) {
    case "MOVE1":
      return { action: "MOVE", steps: 1 };
    case "MOVE2":
      return { action: "MOVE", steps: 2 };
    case "MOVE3":
      return { action: "MOVE", steps: 3 };
    case "MOVEBACK":
      return { action: "MOVE", steps: -1 };
    default:
      // ROTATELEFT/ROTATERIGHT/UTURN/AGAIN/SPEED/SANDBOX/WEASEL/SPAM/TROJAN_HORSE/WORM
      return { action: m, steps: 0 };
  }
}

/** Engine {action, steps} -> client card enum. */
export function cardToMoveType(c: CardSnapshot): MoveType {
  if (c.action === "MOVE") {
    if (c.steps === -1) return "MOVEBACK";
    if (c.steps === 2) return "MOVE2";
    if (c.steps === 3) return "MOVE3";
    return "MOVE1";
  }
  return c.action as MoveType;
}

export function cardsToMoveTypes(cards: CardSnapshot[]): MoveType[] {
  return cards.map(cardToMoveType);
}

export function moveTypesToCards(moves: MoveType[]): CardSnapshot[] {
  return moves.map(moveTypeToCard);
}

// ---- effects -------------------------------------------------------------

function effectToClient(
  e: EffectSnapshot,
  x: number,
  y: number,
  i: number,
): TileEffect | null {
  const id = `${e.kind}-${x}-${y}-${i}`;
  switch (e.kind) {
    case "WALL":
      return { kind: "walldto", id, walls: e.walls };
    case "CHECKPOINT":
      return { kind: "checkpoint", id, number: e.number };
    case "GREEN_CONVEYOR":
      return {
        kind: "GREEN_CONVEYOR",
        id,
        direction: e.direction,
        rotation: e.rotation,
      };
    case "BLUE_CONVEYOR":
      return {
        kind: "BLUE_CONVEYOR",
        id,
        direction: e.direction,
        rotation: e.rotation,
      };
    case "GEAR":
      return { kind: "geardto", id, rotation: e.rotation };
    case "PITS":
      return { kind: "pits", id };
    case "REBOOT_TOKEN":
      return { kind: "reboot_token", id, direction: e.direction };
    case "ANTENNA":
      return { kind: "antenna", id, direction: e.direction };
    case "STARTING_TILE":
      return { kind: "startingtile", id, playerId: e.robotId };
    case "BOARD_LASER":
      return {
        kind: "board_laser",
        id,
        direction: e.direction,
        power: e.power,
      };
    default:
      return null;
  }
}

// ---- board + robots ------------------------------------------------------

function boardToClient(snap: GameSnapshot): Board {
  const tiles: Tile[][] = snap.board.tiles.map((col, x) =>
    col.map((t, y) => ({
      x,
      y,
      effects: t.effects
        .map((e, i) => effectToClient(e, x, y, i))
        .filter((e): e is TileEffect => e !== null),
    })),
  );
  return { width: snap.board.width, height: snap.board.height, tiles };
}

/**
 * Full authoritative render state from a snapshot. Dead (rebooting) robots are
 * omitted so they visually disappear until respawn.
 */
export function snapshotToGameData(snap: GameSnapshot): GameData {
  const robots: Robot[] = snap.robots
    .filter((r) => r.alive)
    .map((r) => ({
      id: r.id,
      x: r.x,
      y: r.y,
      facing: r.facing,
      nextCheckpoint: r.nextCheckpoint,
    }));
  return { board: boardToClient(snap), robots };
}

/**
 * Render state for one activation animation frame: same board, robot positions
 * taken from the frame (nextCheckpoint carried over from the snapshot).
 */
export function frameToGameData(snap: GameSnapshot, frame: Frame): GameData {
  const checkpointById = new Map(
    snap.robots.map((r) => [r.id, r.nextCheckpoint]),
  );
  const robots: Robot[] = frame.robots
    .filter((r) => r.alive)
    .map((r) => ({
      id: r.id,
      x: r.x,
      y: r.y,
      facing: r.facing,
      nextCheckpoint: checkpointById.get(r.id) ?? 1,
    }));
  return { board: boardToClient(snap), robots };
}

/** My hand as client card enums, from the per-robot deck in the snapshot. */
export function handForRobot(snap: GameSnapshot, robotId: number): MoveType[] {
  const deck = snap.decks[String(robotId)];
  return deck ? cardsToMoveTypes(deck.hand) : [];
}

/** My discard pile as client card enums. */
export function discardForRobot(
  snap: GameSnapshot,
  robotId: number,
): MoveType[] {
  const deck = snap.decks[String(robotId)];
  return deck ? cardsToMoveTypes(deck.discardPile) : [];
}
