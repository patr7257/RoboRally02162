export type Direction = "N" | "E" | "S" | "W";

/*
Author(s): Asger, William, Bjarke, Niklas
*/

export interface BaseEffect { kind: string; id: string }
export interface CheckpointEffect extends BaseEffect { kind: "checkpoint"; number: number }
export interface ConveyorEffect   extends BaseEffect { kind: "conveyor"; dir: Direction; speed: 1|2 }
export interface WallEffect       extends BaseEffect { kind: "walldto"; walls: Direction[]; }
export type TileEffect = CheckpointEffect | ConveyorEffect | WallEffect;

export type MoveType =
    | "MOVE1"
    | "MOVE2"
    | "MOVE3"
    | "MOVEBACK"
    | "ROTATERIGHT"
    | "ROTATELEFT"
    | "UTURN";

export interface Tile {
    x: number;
    y: number;
    effects: TileEffect[];
}

export interface Robot {
    id: number;
    x: number;
    y: number;
    facing: Direction;
    nextCheckpoint: number;
}

export interface Board {
    width: number;
    height: number;
    tiles: Tile[][];
}

export interface GameDto {
    gameID: string;
    winner?: number | null;
}

export interface GameData {
    game: GameDto;
    board: Board;
    robots: Robot[];
}

export interface HandData {
  hand: MoveType[];
}


export interface WebSocketMessage {
    type: string;
    payload?: any;
}

export interface GameMessage extends WebSocketMessage {
    type: "game";
    payload: GameData;
}

// Re-export constants for convenience / compatibility
export { ROBOT_COLORS, ROBOT_IMAGES, DIRECTION_ARROWS, MOVE_OPTIONS } from "./constants";