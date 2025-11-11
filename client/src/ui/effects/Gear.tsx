import React from "react";
import { registerEffect } from "../effectRegistry";
import { Rotation } from "../../types/boardTypes";
type GearEffect = { rotation: Rotation };

// Author(s) William

function Gear({ effect }: { effect: GearEffect }) {
  const dirClass =
    effect.rotation === "LEFT" ? "dir-ccw" :
    effect.rotation === "RIGHT" ? "dir-cw"  :
    "";

  return <div className={`tile-effect gear ${dirClass}`} aria-label={`gear-${effect.rotation.toLowerCase()}`} />;
}

registerEffect("geardto", Gear);
