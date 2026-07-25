/**
 * Static catalog of boards the host can pick from when creating a game.
 *
 * Each entry's metadata (displayName/gameLength/difficulty/min-maxPlayers) is
 * copied from the matching board definition JSON at
 * client/public/boards/<id>.json, which is itself a copy of the engine's
 * gateway/src/main/resources/board-templates/<id>.json. Keep these in sync by
 * hand: this file only exists so the picker can render without an async fetch
 * for every card.
 */

export interface BoardCatalogEntry {
  id: string;
  displayName: string;
  preview: string;
  gameLength: string;
  difficulty: string;
  minPlayers: number;
  maxPlayers: number;
}

export const BOARD_CATALOG: BoardCatalogEntry[] = [
  {
    id: "Starter-Course",
    displayName: "Starter-Course",
    preview: `${process.env.PUBLIC_URL}/boardtemplates/Starter-Course.png`,
    gameLength: "Short",
    difficulty: "Beginner",
    minPlayers: 2,
    maxPlayers: 6,
  },
  {
    id: "burnout",
    displayName: "Burnout",
    preview: `${process.env.PUBLIC_URL}/boardtemplates/burnout.png`,
    gameLength: "Medium",
    difficulty: "Intermediate",
    minPlayers: 2,
    maxPlayers: 6,
  },
  {
    id: "death-trap",
    displayName: "Death-Trap",
    preview: `${process.env.PUBLIC_URL}/boardtemplates/death-trap.png`,
    gameLength: "Long",
    difficulty: "Expert",
    minPlayers: 2,
    maxPlayers: 6,
  },
  {
    id: "fractionation",
    displayName: "Fractionation",
    preview: `${process.env.PUBLIC_URL}/boardtemplates/fractionation.png`,
    gameLength: "Long",
    difficulty: "Hard",
    minPlayers: 2,
    maxPlayers: 6,
  },
];

export const DEFAULT_BOARD_ID = "Starter-Course";
