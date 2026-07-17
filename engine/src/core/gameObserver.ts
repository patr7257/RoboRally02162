import type { Game } from "./game.js";

/** Ported from dk.dtu.domain.core.GameObserver. */
export interface GameObserver {
  onWinnerDeclared(game: Game, winner: number): void;
  onGameUpdate(game: Game): void;
}
