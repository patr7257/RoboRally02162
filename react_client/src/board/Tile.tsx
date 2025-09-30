import React from "react";
import Robot from "./Robot";

interface RobotType {
  id: number;
  x: number;
  y: number;
  facing: string;
  [key: string]: any;
}

interface TileProps {
  x: number;
  y: number;
  tile: any; // Replace 'any' with a proper type if you know the tile structure
  robot?: RobotType | null;
  cellSize: number;
}

export default function Tile({ x, y, tile, robot, cellSize }: TileProps) {
  return (
    <div
      style={{
        width: cellSize,
        height: cellSize,
        backgroundColor: robot ? "#ddd" : "#f9f9f9",
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

      {robot && <Robot robot={robot} cellSize={cellSize} />}
    </div>
  );
}
