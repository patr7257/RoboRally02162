import React, { useRef, useState, useEffect } from "react";
import { GameData, Direction, Tile, Robot } from "../types/boardTypes";
import { calculateBoardSize, getRobotAtPosition, getRobotImage } from "../utils/boardUtils";
import RobotLaser from "../ui/effects/RobotLaser";
import { BoardTile } from "./BoardTile";
import "../styles/gameview.css";
import "../styles/boardelements.css";

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
  const rotationHistoryRef = useRef<Record<number, number>>({});
  const containerRef = useRef<HTMLDivElement>(null);
  const [tileSize, setTileSize] = useState<number>(0);

  useEffect(() => {
    if (!gameData || !containerRef.current) return;

    const updateSize = () => {
      const { tileSize: newTileSize } = calculateBoardSize(
        gameData.board.width,
        gameData.board.height,
        containerRef.current
      );
      setTileSize(newTileSize);
    };

    updateSize();

    window.addEventListener('resize', updateSize);
    return () => window.removeEventListener('resize', updateSize);
  }, [gameData]);

  if (!gameData) {
    return <div className="board-empty"><p>Waiting for game data...</p></div>;
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

  if (tileSize === 0) {
    return (
      <div ref={containerRef} className="board-wrapper" style={{ width: '100%', height: '100%' }}>
        <div className="board-empty"><p>Loading board...</p></div>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="board-wrapper" style={{ width: '100%', height: '100%' }}>
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
              board={gameData.board}
              robots={gameData.robots}
              startingAreaInfo={startingAreaInfo ? {
                ...startingAreaInfo,
                boardWidth: gameData.board.width,
                boardHeight: gameData.board.height
              } : null}
              startingAreaBoundary={gameData.board.startingAreaBoundary}
            />
          ))
        )}

        {gameData.robots.map((robot) => (
          <React.Fragment key={robot.id}>
            <div
              className="robot-absolute"
              style={{
                position: 'absolute',
                left: `${robot.x * tileSize}px`,
                top: `${robot.y * tileSize}px`,
                width: `${tileSize}px`,
                height: `${tileSize}px`,
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
            <div
              className="robot-laser-container"
              style={{
                position: 'absolute',
                left: `${robot.x * tileSize}px`,
                top: `${robot.y * tileSize}px`,
                width: `${tileSize}px`,
                height: `${tileSize}px`,
                pointerEvents: 'none',
                zIndex: 999,
              }}
            >
              <RobotLaser
                effect={{
                  kind: "robot_laser",
                  direction: robot.facing,
                  robotId: robot.id,
                  id: `robot-laser-${robot.id}`,
                  x: robot.x,
                  y: robot.y,
                  board: gameData.board,
                  robots: gameData.robots,
                }}
              />
            </div>
          </React.Fragment>
        ))}
      </div>
    </div>
  );
};