import React from "react";
import { GameData } from "../types/boardTypes";
import { calculateBoardSize, getRobotAtPosition, getRobotImage } from "../utils/boardUtils";

import { BoardTile } from "./BoardTile";
import "./board.css";

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Patrick Røbel
* @author William Pii Jæger
* @author Kajsa Alice Ulrika Berlstedt
*/

interface BoardRendererProps { 
  gameData: GameData | null;
  startingAreaInfo?: {
    direction: string;
    width: number;
    height: number;
    boardWidth?: number;
    boardHeight?: number;
  } | null;
}


/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Patrick Røbel
* @author William Pii Jæger
* @author Weihao Mo
* @author Kajsa Alice Ulrika Berlstedt
*/
export const BoardRenderer: React.FC<BoardRendererProps> = ({ gameData, startingAreaInfo }) => {
    const rotationHistoryRef = React.useRef<Record<number, number>>({});

  if (!gameData) {
    return <div className="board-empty"><p>Waiting for game data...</p></div>;
  }

  const { tileSize } = calculateBoardSize(gameData.board.width, gameData.board.height);

  const getRotationDegrees = (facing: string): number => {
  switch (facing) {
    case "N": return 0;
    case "E": return 90;
    case "S": return 180;
    case "W": return 270;
    default: return 0;
  }
};

const getSmoothRotation = (robotId: number, newFacing: string): number => {
    const targetRotation = getRotationDegrees(newFacing);
    const currentRotation = rotationHistoryRef.current[robotId];

    if (currentRotation === undefined) {
      rotationHistoryRef.current[robotId] = targetRotation;
      return targetRotation;
    }

    const delta = ((targetRotation - currentRotation + 540) % 360) - 180;
      const nextRotation = currentRotation + delta;
      rotationHistoryRef.current[robotId] = nextRotation;
      return nextRotation;
    };

  const hasRobotAt = (x: number, y: number): boolean => {
    return gameData.robots.some(robot => robot.x === x && robot.y === y);
  };

  return (
    <div className="board-wrapper">
      <div
        className="board-grid"
        style={{
          gridTemplateColumns: `repeat(${gameData.board.width}, ${tileSize}px)`,
          gridTemplateRows: `repeat(${gameData.board.height}, ${tileSize}px)`,
          ["--tile-size" as any]: `${tileSize}px`,
          position: 'relative',
        }}
      >

        {gameData.board.tiles.map((col, xIdx) =>
          col.map((tile, yIdx) => (
            <BoardTile
              key={`${xIdx}-${yIdx}`}
              x={xIdx}
              y={yIdx}
              tileSize={tileSize}
              robot={null}
              effects={tile.effects}
              hasRobot={hasRobotAt(xIdx, yIdx)}
              startingAreaInfo={startingAreaInfo ? {
                ...startingAreaInfo,
                boardWidth: gameData.board.width,
                boardHeight: gameData.board.height
              } : null}
            />
          ))
        )}
        
        {gameData.robots.map((robot) => (
          <div
            key={robot.id}
            className="robot-absolute"
            style={{
              position: 'absolute',
              left: `${robot.x * tileSize + tileSize/7}px`,
              top: `${robot.y * tileSize + tileSize/7}px`,
              width: `${tileSize}px`,
              height: `${tileSize}px`,
              marginTop: '${-tileSize}px',
              backgroundImage: `url(${getRobotImage(robot.id)})`,
              backgroundSize: "cover",
              backgroundPosition: "center",
              transform: `rotate(${getSmoothRotation(robot.id, robot.facing)}deg)`,
              transition: "left 0.3s ease, top 0.3s ease, transform 0.3s ease",
              transformOrigin: "center center",
              pointerEvents: 'none',
              zIndex: 1000,
              boxSizing: 'border-box',
            }}
          />
        ))}
      </div>
    </div>
  );
};