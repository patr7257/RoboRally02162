import React from "react";
import { Robot, TileEffect } from "../types/boardTypes";
import { getFacingArrow, getRobotColor, getRobotImage } from "../utils/boardUtils";
import { renderEffect } from "../ui/effectRegistry";

/*
Author(s): Bjarke, Asger, Patrick, William, Lizette
*/

interface BoardTileProps {
  x: number;
  y: number;
  tileSize: number;
  robot?: Robot | null;
  effects?: TileEffect[];
}
const getRotationDegrees = (facing: string): number => {
  switch (facing) {
    case "N": return 0;
    case "E": return 90;
    case "S": return 180;
    case "W": return 270;
    default: return 0;
  }
};

export const BoardTile: React.FC<BoardTileProps> = ({
  x, y, tileSize, robot, effects = [],
}) => (
  <div
    className="tile"
    style={{
      width: tileSize,
      height: tileSize,
      gridColumn: x + 1, 
      gridRow: y + 1,
      backgroundImage: `url(${process.env.PUBLIC_URL}/tileTexture.png)`,
      backgroundSize: "cover",
      backgroundPosition: "center",
    }}
  >

    <span className="tile-coords">{x},{y}</span>
    {robot && (
  <div
    className="robot"
    style={{
      backgroundImage: `url(${getRobotImage(robot.id)})`,
      backgroundSize: "cover",
      backgroundPosition: "center",
      width: "100%",
      height: "100%",
      transform: `rotate(${getRotationDegrees(robot.facing)}deg)`,
      transition: "transform 0.3s ease",
      transformOrigin: "center center",
    }}
  />
)}

    {effects.map(e => (
      <div key={e.id} className={`effect-wrapper kind-${e.kind}`}>
        {renderEffect(e)}
      </div>
    ))}
  </div>
);