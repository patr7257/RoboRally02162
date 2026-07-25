import type { HostPlayerConfig, LoadedBoard } from "../engine/roborally-engine";
import { ROBOT_COLORS } from "../types/constants";

/**
 * Builds the HostPlayerConfig list createGame needs from the lobby roster and
 * a loaded board definition. Extracted out of CreateJoin's onStart so a later
 * rematch feature (re-running createGame against the same roster on a new
 * board) can reuse it without duplicating the mapping logic.
 */
export function buildPlayerConfigs(
  loaded: LoadedBoard,
  players: { idx: number; name: string }[],
): HostPlayerConfig[] {
  return players.map((p) => {
    const robotId = p.idx + 1;
    const tile = loaded.startingTiles[robotId] || { x: 0, y: p.idx };
    return {
      robotId,
      name: p.name,
      color: ROBOT_COLORS[p.idx % ROBOT_COLORS.length],
      x: tile.x,
      y: tile.y,
      facing: loaded.startDirection,
    };
  });
}
