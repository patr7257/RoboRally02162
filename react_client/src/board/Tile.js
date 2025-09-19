import React from "react";
import Robot from "./Robot";

export default function Tile({ x, y, tile, robot, cellSize }) {
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
