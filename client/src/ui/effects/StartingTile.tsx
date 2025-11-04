import React from "react";
import { registerEffect } from "../effectRegistry";
import type { StartingTileEffect } from "../../types/boardTypes";

// Author(s) Patrick Røbel

export default function StartingTile({ effect }: { effect: StartingTileEffect }) {
  return (
    <div className="starting-tile">
      <div className="player-indicator">P{effect.playerId}</div>
    </div>
  );
}
registerEffect("startingtile", StartingTile);