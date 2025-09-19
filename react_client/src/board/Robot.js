import React from "react";
import { getFacingArrow, getRobotColor } from "./boardUtils";

export default function Robot({ robot, cellSize }) {
  return (
    <div
      style={{
        textAlign: "center",
        color: "white",
        fontWeight: "bold",
        backgroundColor: getRobotColor(robot.id),
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <div style={{ fontSize: Math.max(14, cellSize * 0.4) }}>
        {getFacingArrow(robot.facing)}
      </div>
      <div style={{ fontSize: Math.max(8, cellSize * 0.2) }}>
        R{robot.id}
      </div>
    </div>
  );
}
