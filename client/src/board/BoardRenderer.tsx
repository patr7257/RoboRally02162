import React from "react";
import { GameData } from "../types/boardTypes";
import { calculateBoardSize, getRobotAtPosition } from "../utils/boardUtils";
import { BoardTile } from "./BoardTile";
import "./board.css";

/*
Author(s): Bjarke, Asger, Patrick, William
*/

interface BoardRendererProps { gameData: GameData | null; }

export const BoardRenderer: React.FC<BoardRendererProps> = ({ gameData }) => {
  if (!gameData) {
    return <div className="board-empty"><p>Waiting for game data...</p></div>;
  }

  const { tileSize } = calculateBoardSize(gameData.board.width, gameData.board.height);

  return (
    <div className="board-wrapper">
      <div className="board-info">
        <p>
          Board: {gameData.board.width}x{gameData.board.height} | Robots: {gameData.robots.length}
        </p>
      </div>

      <div
        className="board-grid"
        style={{
          gridTemplateColumns: `repeat(${gameData.board.width}, ${tileSize}px)`,
          gridTemplateRows: `repeat(${gameData.board.height}, ${tileSize}px)`,
          ["--tile-size" as any]: `${tileSize}px`,
        }}
      >
        {gameData.board.tiles.map((row, y) =>
          row.map((tile, x) => {
            const robot = getRobotAtPosition(gameData.robots, x, y);
            return (
              <BoardTile
                key={`${x}-${y}`}
                x={x}
                y={y}
                tileSize={tileSize}
                robot={robot}
                effects={tile.effects}
              />
            );
          })
        )}
      </div>
    </div>
  );
};
