import React from "react";
import { registerEffect } from "../effectRegistry";
import type { StartingTileEffect } from "../../types/boardTypes";

/**
 * @author Patrick Røbel
 */
export default function StartingTile({ effect }: { effect: StartingTileEffect }) {
  return (
    <div className="starting-tile" />
  );
}
registerEffect("startingtile", StartingTile);