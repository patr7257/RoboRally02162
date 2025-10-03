// src/types.ts
export interface Robot {
  id: number;
  x: number;
  y: number;
  facing: string;
  [key: string]: any;
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
  game : GameDto
  board: Board;
  robots?: Robot[];
  [key: string]: any;
}

export type EffectDto =
| { kind : "CHECKPOINT"; number : number}


export interface Tile {
    effects: EffectDto[];
}



