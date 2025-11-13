import React from "react";
import { registerEffect } from "../effectRegistry";
import type { BlueConveyorEffect } from "../../types/boardTypes";

/**
 * @author Weihao Mo
 */
const BLUE_CONVEYOR_IMAGES: Record<string, string> = {
  NONE: "/conveyors/blue-n-none.png",
  RIGHT: "/conveyors/blue-n-right.png",
  LEFT: "/conveyors/blue-n-left.png",
};

const DIRECTION_ROTATION: Record<string, number> = {
  N: 0,
  E: 90,
  S: 180,
  W: 270,
};

function BlueConveyor({ effect }: { effect: BlueConveyorEffect }) {
  const imageSrc = BLUE_CONVEYOR_IMAGES[effect.rotation];
  const rotationDegrees = DIRECTION_ROTATION[effect.direction];

  return (
    <div
      className="conveyor conveyor-blue"
      aria-label={`blue conveyor ${effect.direction.toLowerCase()} ${effect.rotation.toLowerCase()}`}
      style={{
        position: "absolute",
        inset: 0,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1,
      }}
    >
      <img
        src={imageSrc}
        alt={`Blue conveyor ${effect.direction} ${effect.rotation}`}
        style={{
          width: "100%",
          height: "100%",
          objectFit: "contain",
          transform: `rotate(${rotationDegrees}deg)`,
        }}
      />
    </div>
  );
}

registerEffect("BLUE_CONVEYOR", BlueConveyor);
export {};