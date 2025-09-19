import React from "react";
import Tile from "./Tile";

export default function BoardGrid({ board, robots, cellSize }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(${board.width}, ${cellSize}px)`,
        gridTemplateRows: `repeat(${board.height}, ${cellSize}px)`,
        border: "2px solid #333",
        backgroundColor: "#ccc",
        gap: "1px",
      }}
    >
      {board.tiles.map((row, y) =>
        row.map((tile, x) => {
          const robot = robots?.find((r) => r.x === x && r.y === y);
          return (
            <Tile
              key={`${x}-${y}`}
              x={x}
              y={y}
              tile={tile}
              robot={robot}
              cellSize={cellSize}
            />
          );
        })
      )}
    </div>
  );
}
