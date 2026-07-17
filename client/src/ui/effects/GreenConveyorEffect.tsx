import React from "react";
import { registerEffect } from "../effectRegistry";
import type { GreenConveyorEffect } from "../../types/boardTypes";

/**
 *@author Weihao Mo
 */
const GREEN_CONVEYOR_IMAGES: Record<string, string> = {
  RIGHT: `${process.env.PUBLIC_URL || ""}/boardelements/conveyors/green-n-right.png`,
  LEFT: `${process.env.PUBLIC_URL || ""}/boardelements/conveyors/green-n-left.png`,
  NONE: `${process.env.PUBLIC_URL || ""}/boardelements/conveyors/green-n-none.png`,
};

/**
 *@author Weihao Mo
 */
const DIRECTION_ROTATION: Record<string, number> = {
  N: 0,
  E: 90,
  S: 180,
  W: 270,
};

/**
 *@author Weihao Mo
 */
function GreenConveyor({ effect }: { effect: GreenConveyorEffect }) {
  const imageSrc = GREEN_CONVEYOR_IMAGES[effect.rotation];
  const rotationDegrees = DIRECTION_ROTATION[effect.direction];

  return (
    <div
      className="conveyor conveyor-green"
      aria-label={`green conveyor ${effect.direction.toLowerCase()} ${effect.rotation.toLowerCase()}`}
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
        alt={`Green conveyor ${effect.direction} ${effect.rotation}`}
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

registerEffect("GREEN_CONVEYOR", GreenConveyor);
export { };