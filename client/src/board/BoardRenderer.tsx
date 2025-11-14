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
*/

interface BoardRendererProps { gameData: GameData | null; }


/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Patrick Røbel
* @author William Pii Jæger
*/
export const BoardRenderer: React.FC<BoardRendererProps> = ({ gameData }) => {
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
              transform: `rotate(${getRotationDegrees(robot.facing)}deg)`,
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