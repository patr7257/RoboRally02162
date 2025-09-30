import { getRobotColor, getFacingArrow } from "./boardUtils";
import { GameData, Robot } from "./Types";

interface BoardRendererProps {
  gameData: GameData | null;
}

export default function BoardRenderer({ gameData }: BoardRendererProps) {
  const calculateBoardSize = () => {
    if (!gameData) return { cellSize: 0, boardWidth: 0, boardHeight: 0 };

    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    const availableWidth = viewportWidth * 0.7;
    const availableHeight = viewportHeight * 0.7;

    const { width, height } = gameData.board;

    const cellSizeByWidth = availableWidth / width;
    const cellSizeByHeight = availableHeight / height;
    const cellSize = Math.min(cellSizeByWidth, cellSizeByHeight, 60); // Max 60px per cell

    return {
      cellSize: Math.floor(cellSize),
      boardWidth: width * Math.floor(cellSize),
      boardHeight: height * Math.floor(cellSize),
    };
  };

  const getRobotAtPosition = (x: number, y: number): Robot | null => {
    if (!gameData?.robots) return null;
    return gameData.robots.find((robot) => robot.x === x && robot.y === y) || null;
  };

  if (!gameData) {
    return (
      <div style={{ textAlign: "center", padding: "20px" }}>
        <p>Waiting for game data...</p>
      </div>
    );
  }

  const { cellSize, boardWidth, boardHeight } = calculateBoardSize();

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        margin: "20px 0",
      }}
    >
      <div style={{ marginBottom: "10px" }}>
        <p>
          Board: {gameData.board.width}×{gameData.board.height} | Robots:{" "}
          {gameData.robots?.length || 0}
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: `repeat(${gameData.board.width}, ${cellSize}px)`,
          gridTemplateRows: `repeat(${gameData.board.height}, ${cellSize}px)`,
          border: "2px solid #333",
          backgroundColor: "#ccc",
          gap: "1px",
        }}
      >
        {gameData.board.tiles.map((row, y) =>
          row.map((tile, x) => {
            const robot = getRobotAtPosition(x, y);

            return (
              <div
                key={`${x}-${y}`}
                style={{
                  width: cellSize,
                  height: cellSize,
                  backgroundColor: robot ? getRobotColor(robot.id) : "#f9f9f9",
                  border: "1px solid #ddd",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  position: "relative",
                  fontSize: Math.max(12, cellSize * 0.3),
                }}
              >
                <span
                  style={{
                    position: "absolute",
                    top: "2px",
                    left: "2px",
                    fontSize: Math.max(8, cellSize * 0.15),
                    color: "#666",
                  }}
                >
                  {x},{y}
                </span>

                {robot && (
                  <div
                    style={{
                      textAlign: "center",
                      color: "white",
                      fontWeight: "bold",
                    }}
                  >
                    <div style={{ fontSize: Math.max(14, cellSize * 0.4) }}>
                      {getFacingArrow(robot.facing)}
                    </div>
                    <div style={{ fontSize: Math.max(8, cellSize * 0.2) }}>
                      R{robot.id}
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
