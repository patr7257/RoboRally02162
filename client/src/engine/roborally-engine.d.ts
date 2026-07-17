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

export interface HostPlayerConfig {
  robotId: number;
  name: string;
  color: string;
  x: number;
  y: number;
  facing: Direction;
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
export function runActivation(snapshot: GameSnapshot): ActivationResult;
