export type Direction = "N" | "E" | "S" | "W";
export type Rotation = "NONE" | "LEFT" | "RIGHT";

/** 
* @author Bjarke Søderhamn Petersen
* @author Asger Allin Jensen
* @author William Pii Jæger
* @author Patrick Røbel
* @author Weihao Mo
*/

export interface BaseEffect { kind: string; id: string }
export interface CheckpointEffect extends BaseEffect { kind: "checkpoint"; number: number }
export interface BlueConveyorEffect extends BaseEffect { kind: "BLUE_CONVEYOR"; direction: Direction; rotation: Rotation; }
export interface GreenConveyorEffect extends BaseEffect { kind: "GREEN_CONVEYOR"; direction: Direction; rotation: Rotation; }
export interface WallEffect       extends BaseEffect { kind: "walldto"; walls: Direction[]; }
export interface GearEffect       extends BaseEffect { kind: "geardto"; rotation: Rotation; }
export interface RebootTokenEffect extends BaseEffect { kind: "reboot_token"; direction: Direction}
export interface StartingTileEffect extends BaseEffect { kind: "startingtile"; playerId: number; }
export interface AntennaEffect extends BaseEffect { kind : "antenna"; direction: Direction}
export interface PitsEffect extends BaseEffect { kind : "pits";}
export type TileEffect = CheckpointEffect | BlueConveyorEffect | GreenConveyorEffect  | WallEffect | StartingTileEffect | RebootTokenEffect | AntennaEffect | GearEffect | PitsEffect;

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

export interface BoardTemplateInfo {
    name: string;           // Template identifier (filename)
    displayName?: string;   // Optional pretty name for display
    difficulty: string;
    maxPlayers: number;
    gameLength: string;
    imageUrl: string;
}

export interface HandData {
  hand: MoveType[];
}

export interface DiscardData {
  discard: MoveType[];
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