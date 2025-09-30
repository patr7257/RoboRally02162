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
  tiles: any[][];
}

export interface GameData {
  board: Board;
  robots?: Robot[];
  [key: string]: any;
}
