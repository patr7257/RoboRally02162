import React from "react";
import { Robot, TileEffect } from "../types/boardTypes";
import { getFacingArrow, getRobotColor, getRobotImage } from "../utils/boardUtils";
import { renderEffect } from "../ui/effectRegistry";


/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Nikolajsen
* @author Patrick Røbel
* @author William Pii Jæger
* @author Weihao Mo
*/
interface BoardTileProps {
  x: number;
  y: number;
  tileSize: number;
  robot?: Robot | null;
  effects?: TileEffect[];
  board?: { tiles: any[][]; width: number; height: number };
  robots?: Robot[];
  startingAreaInfo?: {
    direction: string;
    width: number;
    height: number;
    boardWidth?: number;
    boardHeight?: number;
  } | null;
  startingAreaBoundary?: {
    direction: 'N' | 'S' | 'E' | 'W';
    position: number;
  };
}
/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
const getRotationDegrees = (facing: string): number => {
  switch (facing) {
    case "N": return 0;
    case "E": return 90;
    case "S": return 180;
    case "W": return 270;
    default: return 0;
  }
};

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Nikolajsen
* @author Patrick Røbel
* @author William Pii Jæger
*/
export const BoardTile: React.FC<BoardTileProps> = ({
  x, y, tileSize, robot, effects = [], board, robots, startingAreaInfo = null,
}) => {
  const isInStartingArea = (): boolean => {
    if (!startingAreaInfo) return false;

    const { direction, width, height, boardWidth, boardHeight } = startingAreaInfo;

    switch (direction) {
      case 'W': return x < width;
      case 'E': return boardWidth ? x >= (boardWidth - width) : false;
      case 'N': return y < height;
      case 'S': return boardHeight ? y >= (boardHeight - height) : false;
      default: return false;
    }
  };

  return (
  <div
    className={`tile ${isInStartingArea() ? 'starting-area-tile' : ''}`}
    style={{
      width: tileSize,
      height: tileSize,
      gridColumn: x + 1,
      gridRow: y + 1,
      backgroundImage: `url(${process.env.PUBLIC_URL}/boardelements/others/tileTexture.png)`,
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
        {renderEffect({ ...e, x, y, board, robots } as any)}
      </div>
    ))}
  </div>
  );
};