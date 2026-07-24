// Hand-authored type declarations for the copied engine ESM bundle
// (roborally-engine.js). Kept in sync by hand with engine/src/host/*.ts and
// engine/src/program/programCard.ts. Only the public surface the client uses is
// declared here; the bundle exports more (see engine/src/index.ts).

export type Direction = "N" | "E" | "S" | "W";
export type Rotation = "NONE" | "LEFT" | "RIGHT";

export const Action: {
  readonly MOVE: "MOVE";
  readonly ROTATERIGHT: "ROTATERIGHT";
  readonly ROTATELEFT: "ROTATELEFT";
  readonly UTURN: "UTURN";
  readonly SPAM: "SPAM";
  readonly TROJAN_HORSE: "TROJAN_HORSE";
  readonly WORM: "WORM";
  readonly SANDBOX: "SANDBOX";
  readonly WEASEL: "WEASEL";
  readonly SPEED: "SPEED";
  readonly AGAIN: "AGAIN";
};
export type Action = (typeof Action)[keyof typeof Action];

export type GameStatus =
  | "lobby"
  | "programming"
  | "activating"
  | "awaiting-reaction"
  | "awaiting-respawn"
  | "finished";

export interface CardSnapshot {
  action: Action;
  steps: number;
}

export type ReactionKind = "SANDBOX" | "WEASEL" | "SPEED";

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

/** A reaction the host is waiting on; promptId is deterministic per pause. */
export interface PendingReaction {
  /** `r<round>-g<register>-t<turnIndex>-<robotId>`. */
  promptId: string;
  robotId: number;
  /** 1..5 */
  register: number;
  kind: ReactionKind;
  options: ReactionChoice[];
  defaultChoice: ReactionChoice;
}

/**
 * Where an interrupted activation stands: turnOrder is frozen at register start
 * and turnIndex points at the paused robot.
 */
export interface ActivationCursor {
  register: number;
  turnOrder: number[];
  turnIndex: number;
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
  /** Mid-activation state, present only while an activation is paused. */
  registers?: CardSnapshot[];
  lastExecuted?: CardSnapshot | null;
  movedOnActivation?: boolean;
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
  /** Present while status is "awaiting-reaction". */
  activation?: ActivationCursor | null;
  pendingReaction?: PendingReaction | null;
}

export interface HostPlayerConfig {
  robotId: number;
  name: string;
  color: string;
  x: number;
  y: number;
  facing: Direction;
}

/** What the robots were doing when a frame was captured. */
export interface FrameLabel {
  /** null for board-effect and end-of-register frames. */
  robotId: number | null;
  /** 1..5 during activation, 0 during the reboot phase. */
  register: number;
  /** Card name ("MOVE2"), resolved reaction choice, "BOARD" or "REBOOT". */
  text: string;
}

/** One animation frame: robot positions/headings at a point during activation. */
export interface Frame {
  robots: {
    id: number;
    x: number;
    y: number;
    facing: Direction;
    alive: boolean;
  }[];
  label?: FrameLabel;
}

export interface ActivationResult {
  snapshot: GameSnapshot;
  frames: Frame[];
}

export interface BoardDefinition {
  displayName?: string;
  boardWidth?: number;
  boardHeight?: number;
  startingBoardWidth?: number;
  startingBoardHeight?: number;
  startingBoardDirection?: string;
  effects?: Record<string, unknown>;
}

export interface LoadedBoard {
  displayName: string;
  board: BoardSnapshot;
  /** 1-based playerId -> starting coordinate. */
  startingTiles: Record<number, { x: number; y: number }>;
  startDirection: Direction;
}

export function parseBoardDefinition(def: BoardDefinition): LoadedBoard;
export function createGame(
  board: BoardSnapshot,
  players: HostPlayerConfig[],
): GameSnapshot;
export function submitProgram(
  snapshot: GameSnapshot,
  robotId: number,
  cards: CardSnapshot[],
): GameSnapshot;
export function allSubmitted(snapshot: GameSnapshot): boolean;
/** Runs a round from register 1; throws on an already paused snapshot. */
export function runActivation(snapshot: GameSnapshot): ActivationResult;
/**
 * Applies a reaction choice to an "awaiting-reaction" snapshot and continues the
 * round. A missing or illegal choice falls back to the reaction default.
 */
export function resumeActivation(
  snapshot: GameSnapshot,
  choice?: ReactionChoice | null,
): ActivationResult;
/**
 * Reboots every dead robot of an "awaiting-respawn" snapshot, then deals new
 * hands and returns to programming. A robot with no entry keeps the facing it
 * died with.
 */
export function applyRespawns(
  snapshot: GameSnapshot,
  directions: Record<number, Direction>,
): ActivationResult;
/** Oracle options and defaults per reaction kind. */
export const REACTION_SPECS: Record<ReactionKind, ReactionSpec>;
export function normalizeChoice(
  kind: ReactionKind,
  choice: ReactionChoice | null | undefined,
): ReactionChoice;
