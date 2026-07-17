import { Board } from "../model/board.js";
import { Tile } from "../model/tile.js";
import { Robot } from "../model/robot.js";
import { Deck } from "../model/deck.js";
import { DamageDecks } from "../model/damageDecks.js";
import { Direction } from "../model/direction.js";
import { Rotation } from "../model/rotation.js";
import { Action, ProgramCard } from "../program/programCard.js";
import type { TileEffect } from "../effects/tileEffect.js";
import { Walls } from "../effects/walls.js";
import { Checkpoint } from "../effects/checkpoint.js";
import { GreenConveyor, BlueConveyor } from "../effects/conveyors.js";
import { Gear } from "../effects/gear.js";
import { Pits } from "../effects/pits.js";
import { RebootToken } from "../effects/rebootToken.js";
import { Antenna } from "../effects/antenna.js";
import { StartingTile } from "../effects/startingTile.js";
import { BoardLaser } from "../effects/boardLaser.js";

/**
 * Serializable snapshot schema for the host-authoritative browser loop.
 *
 * This is the JSON blob that the lobby-creator PUTs to the Vercel backend and
 * re-reads on resume. It is a superset of the Java SnapshotPayload: besides
 * board + robots it also carries decks, the global damage pools, per-player
 * program state, and the round/status, so a round can be resumed from state
 * alone. Dynamic RobotLaser effects are never persisted (added and removed
 * within a single activation), matching the Java SnapshotMapper.
 */

export type GameStatus = "lobby" | "programming" | "activating" | "finished";

export interface CardSnapshot {
  action: Action;
  steps: number;
}

export type EffectSnapshot =
  | { kind: "WALL"; walls: Direction[] }
  | { kind: "CHECKPOINT"; number: number }
  | { kind: "GREEN_CONVEYOR"; direction: Direction; rotation: Rotation }
  | { kind: "BLUE_CONVEYOR"; direction: Direction; rotation: Rotation }
  | { kind: "GEAR"; rotation: Rotation }
  | { kind: "PITS" }
  | { kind: "REBOOT_TOKEN"; direction: Direction }
  | { kind: "ANTENNA"; direction: Direction }
  | { kind: "STARTING_TILE"; robotId: number }
  | { kind: "BOARD_LASER"; direction: Direction; power: number };

export interface TileSnapshot {
  effects: EffectSnapshot[];
}

export interface BoardSnapshot {
  width: number;
  height: number;
  /** Column-major: tiles[x][y]. */
  tiles: TileSnapshot[][];
}

export interface RobotSnapshot {
  id: number;
  x: number;
  y: number;
  facing: Direction;
  nextCheckpoint: number;
  alive: boolean;
  respawnDirection: Direction | null;
}

export interface DeckSnapshot {
  drawPile: CardSnapshot[];
  discardPile: CardSnapshot[];
  hand: CardSnapshot[];
}

export interface DamageDecksSnapshot {
  spam: number;
  trojan: number;
  worm: number;
}

export interface PlayerSnapshot {
  robotId: number;
  name: string;
  color: string;
  program: CardSnapshot[] | null;
  locked: boolean;
}

export interface GameSnapshot {
  status: GameStatus;
  round: number;
  board: BoardSnapshot;
  robots: RobotSnapshot[];
  decks: Record<string, DeckSnapshot>;
  damageDecks: DamageDecksSnapshot;
  players: PlayerSnapshot[];
  winner: number | null;
}

// --- cards ---

export function cardToSnapshot(card: ProgramCard): CardSnapshot {
  return { action: card.action, steps: card.steps };
}

export function cardFromSnapshot(snap: CardSnapshot): ProgramCard {
  return new ProgramCard(snap.action, snap.steps);
}

// --- effects ---

export function effectToSnapshot(effect: TileEffect): EffectSnapshot | null {
  if (effect instanceof Walls) return { kind: "WALL", walls: [...effect.edges] };
  if (effect instanceof Checkpoint)
    return { kind: "CHECKPOINT", number: effect.number };
  if (effect instanceof GreenConveyor)
    return {
      kind: "GREEN_CONVEYOR",
      direction: effect.direction,
      rotation: effect.rotation,
    };
  if (effect instanceof BlueConveyor)
    return {
      kind: "BLUE_CONVEYOR",
      direction: effect.direction,
      rotation: effect.rotation,
    };
  if (effect instanceof Gear) return { kind: "GEAR", rotation: effect.rotation };
  if (effect instanceof Pits) return { kind: "PITS" };
  if (effect instanceof RebootToken)
    return { kind: "REBOOT_TOKEN", direction: effect.direction };
  if (effect instanceof Antenna)
    return { kind: "ANTENNA", direction: effect.direction };
  if (effect instanceof StartingTile)
    return { kind: "STARTING_TILE", robotId: effect.robotId };
  if (effect instanceof BoardLaser)
    return { kind: "BOARD_LASER", direction: effect.direction, power: effect.power };
  // RobotLaser is dynamic and never persisted.
  return null;
}

export function effectFromSnapshot(snap: EffectSnapshot): TileEffect {
  switch (snap.kind) {
    case "WALL":
      return new Walls(snap.walls);
    case "CHECKPOINT":
      return new Checkpoint(snap.number);
    case "GREEN_CONVEYOR":
      return new GreenConveyor(snap.direction, snap.rotation);
    case "BLUE_CONVEYOR":
      return new BlueConveyor(snap.direction, snap.rotation);
    case "GEAR":
      return new Gear(snap.rotation);
    case "PITS":
      return new Pits();
    case "REBOOT_TOKEN":
      return new RebootToken(snap.direction);
    case "ANTENNA":
      return new Antenna(snap.direction);
    case "STARTING_TILE":
      return new StartingTile(snap.robotId);
    case "BOARD_LASER":
      return new BoardLaser(snap.direction, snap.power);
  }
}

// --- board ---

export function boardToSnapshot(board: Board): BoardSnapshot {
  const tiles: TileSnapshot[][] = [];
  for (let x = 0; x < board.getWidth(); x++) {
    tiles[x] = [];
    for (let y = 0; y < board.getHeight(); y++) {
      const tile = board.getTile(x, y);
      const effects: EffectSnapshot[] = [];
      for (const e of tile.getEffects()) {
        const snap = effectToSnapshot(e);
        if (snap !== null) effects.push(snap);
      }
      tiles[x][y] = { effects };
    }
  }
  return { width: board.getWidth(), height: board.getHeight(), tiles };
}

export function boardFromSnapshot(snap: BoardSnapshot): Board {
  const tiles: Tile[][] = [];
  for (let x = 0; x < snap.width; x++) {
    tiles[x] = [];
    for (let y = 0; y < snap.height; y++) {
      const effects = snap.tiles[x][y].effects.map(effectFromSnapshot);
      tiles[x][y] = new Tile(x, y, effects);
    }
  }
  return new Board(snap.width, snap.height, tiles);
}

// --- robots ---

export function robotToSnapshot(robot: Robot): RobotSnapshot {
  return {
    id: robot.getId(),
    x: robot.getX(),
    y: robot.getY(),
    facing: robot.getDirection(),
    nextCheckpoint: robot.getNextCheckpoint(),
    alive: robot.isAlive(),
    respawnDirection: robot.getRespawnDirection(),
  };
}

export function robotFromSnapshot(snap: RobotSnapshot): Robot {
  const robot = new Robot(snap.id, snap.x, snap.y, snap.facing, snap.nextCheckpoint);
  if (!snap.alive) robot.setDead();
  if (snap.respawnDirection !== null)
    robot.setRespawnDirection(snap.respawnDirection);
  return robot;
}

// --- decks ---

export function deckToSnapshot(deck: Deck): DeckSnapshot {
  return {
    drawPile: deck.getDrawPile().map(cardToSnapshot),
    discardPile: deck.getDiscardPile().map(cardToSnapshot),
    hand: deck.getHand().map(cardToSnapshot),
  };
}

export function deckFromSnapshot(snap: DeckSnapshot, damageDecks: DamageDecks): Deck {
  return new Deck(
    snap.drawPile.map(cardFromSnapshot),
    snap.discardPile.map(cardFromSnapshot),
    snap.hand.map(cardFromSnapshot),
    damageDecks,
  );
}

// --- damage decks ---

export function damageDecksToSnapshot(dd: DamageDecks): DamageDecksSnapshot {
  return {
    spam: dd.getSpamDrawPile(),
    trojan: dd.getTrojanHorseDrawPile(),
    worm: dd.getWormDrawPile(),
  };
}

export function damageDecksFromSnapshot(snap: DamageDecksSnapshot): DamageDecks {
  return new DamageDecks(snap.spam, snap.trojan, snap.worm);
}
