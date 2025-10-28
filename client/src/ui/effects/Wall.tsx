import React from "react";
import { registerEffect } from "../effectRegistry";
import type { WallEffect } from "../../types/boardTypes";

// Author(s) William

export default function Wall({ effect }: { effect: WallEffect }) {
  const dirClasses = effect.walls
    .map(d => `dir-${d.toLowerCase()}`)
    .join(' ');

  return <div className={`wall ${dirClasses}`} />;
}
registerEffect("walldto", Wall);