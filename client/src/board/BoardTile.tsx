import React from "react";
import { Robot, TileEffect } from "../types/boardTypes";
import { getFacingArrow, getRobotColor } from "../utils/boardUtils";
import { renderEffect } from "../ui/effectRegistry";

/*
Author(s): Bjarke, Asger, Patrick, William
*/

interface BoardTileProps {
  x: number;
  y: number;
  tileSize: number;
  robot?: Robot | null;
  effects?: TileEffect[];
}

export const BoardTile: React.FC<BoardTileProps> = ({
  x, y, tileSize, robot, effects = [],
}) => (
<div className="tile" style={{ width: tileSize, height: tileSize, gridColumn: x + 1, gridRow: y + 1 }}>
    <span className="tile-coords">{x},{y}</span>
    {robot && (
      <div className="robot" style={{ backgroundColor: getRobotColor(robot.id) }}>
        <div className="robot-arrow">{getFacingArrow(robot.facing)}</div>
        <div className="robot-id">R{robot.id}</div>
      </div>
    )}

    {effects.map(e => (
      <div key={e.id} className={`effect-wrapper kind-${e.kind}`}>
        {renderEffect(e)}
      </div>
    ))}
  </div>
);
