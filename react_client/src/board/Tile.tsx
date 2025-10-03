import React from "react";
import Robot from "./Robot";
import { Tile as TileType, EffectDto } from "./Types";

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
  tile: TileType;
  robot?: RobotType | null;
  cellSize: number;
}

export default function Tile({ x, y, tile, robot, cellSize }: TileProps) {
    console.log(`Tile at (${x},${y}):`, tile.effects);

    const checkpoint = tile.effects.find(
      (e): e is { kind: "CHECKPOINT"; number: number } => e.kind === "CHECKPOINT"
    );


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


      {checkpoint && (
        <div
          style={{
            position: "absolute",
            bottom: "2px",
            right: "2px",
            backgroundColor: "gold",
            color: "black",
            fontWeight: "bold",
            padding: "2px 4px",
            borderRadius: "50%",
            fontSize: Math.max(10, cellSize * 0.25),
            zIndex: 10,
          }}
        >
          {checkpoint.number}
        </div>
      )}
    </div>
  );
}
