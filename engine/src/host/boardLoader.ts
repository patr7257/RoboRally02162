import { Direction } from "../model/direction.js";
import { Rotation } from "../model/rotation.js";
import type { BoardSnapshot, EffectSnapshot, TileSnapshot } from "./snapshot.js";

/**
 * Loads the JSON board-definition format used by client/public/board.json and
 * the gateway board-templates into a BoardSnapshot the engine can consume.
 *
 * The definition places effects by absolute "x,y" key on a grid whose width is
 * startingBoardWidth + boardWidth (the starting area sits in the leftmost
 * columns) and whose height is boardHeight. Effect `kind` strings are
 * case-insensitive and mapped to the engine's clean EffectSnapshot kinds.
 */

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

function asDirection(v: unknown): Direction {
  const s = String(v).toUpperCase();
  if (s === "N" || s === "E" || s === "S" || s === "W") return s;
  throw new Error("Invalid direction: " + v);
}

function asRotation(v: unknown): Rotation {
  if (v == null) return Rotation.NONE;
  const s = String(v).toUpperCase();
  if (s === "NONE" || s === "LEFT" || s === "RIGHT") return s;
  throw new Error("Invalid rotation: " + v);
}

/** Maps one raw effect object to an EffectSnapshot, or null to skip it. */
function toEffectSnapshot(raw: Record<string, unknown>): EffectSnapshot | null {
  const kind = String(raw.kind ?? "").toLowerCase();
  switch (kind) {
    case "wall":
    case "walldto":
      return {
        kind: "WALL",
        walls: (Array.isArray(raw.walls) ? raw.walls : []).map(asDirection),
      };
    case "checkpoint":
      return { kind: "CHECKPOINT", number: Number(raw.number) };
    case "green_conveyor":
    case "greenconveyor":
    case "greenconveyordto":
      return {
        kind: "GREEN_CONVEYOR",
        direction: asDirection(raw.direction),
        rotation: asRotation(raw.rotation),
      };
    case "blue_conveyor":
    case "blueconveyor":
    case "blueconveyordto":
      return {
        kind: "BLUE_CONVEYOR",
        direction: asDirection(raw.direction),
        rotation: asRotation(raw.rotation),
      };
    case "gear":
    case "geardto":
      return { kind: "GEAR", rotation: asRotation(raw.rotation) };
    case "pits":
    case "pitsdto":
      return { kind: "PITS" };
    case "reboot_token":
    case "reboottoken":
      return { kind: "REBOOT_TOKEN", direction: asDirection(raw.direction) };
    case "antenna":
      return { kind: "ANTENNA", direction: asDirection(raw.direction) };
    case "startingtile":
      return { kind: "STARTING_TILE", robotId: Number(raw.playerId) };
    case "board_laser":
    case "boardlaser":
      return {
        kind: "BOARD_LASER",
        direction: asDirection(raw.direction),
        power: Number(raw.power),
      };
    // robot_laser is dynamic and never part of a static board.
    default:
      return null;
  }
}

export function parseBoardDefinition(def: BoardDefinition): LoadedBoard {
  const width = (def.startingBoardWidth ?? 0) + (def.boardWidth ?? 0);
  const height = def.boardHeight ?? 0;
  if (width <= 0 || height <= 0) {
    throw new Error("Board definition has no dimensions");
  }

  const tiles: TileSnapshot[][] = [];
  for (let x = 0; x < width; x++) {
    tiles[x] = [];
    for (let y = 0; y < height; y++) tiles[x][y] = { effects: [] };
  }

  const startingTiles: Record<number, { x: number; y: number }> = {};
  const effects = def.effects ?? {};

  for (const key of Object.keys(effects)) {
    const value = effects[key];
    if (!Array.isArray(value)) continue; // skip _comment_* string entries
    const parts = key.split(",");
    if (parts.length !== 2) continue;
    const x = Number(parts[0]);
    const y = Number(parts[1]);
    if (!Number.isInteger(x) || !Number.isInteger(y)) continue;
    if (x < 0 || y < 0 || x >= width || y >= height) continue;

    for (const rawEffect of value) {
      if (typeof rawEffect !== "object" || rawEffect === null) continue;
      const raw = rawEffect as Record<string, unknown>;
      const snap = toEffectSnapshot(raw);
      if (snap === null) continue;
      tiles[x][y].effects.push(snap);
      if (snap.kind === "STARTING_TILE") {
        startingTiles[snap.robotId] = { x, y };
      }
    }
  }

  return {
    displayName: def.displayName ?? "board",
    board: { width, height, tiles },
    startingTiles,
    startDirection: asDirection(def.startingBoardDirection ?? "N"),
  };
}
