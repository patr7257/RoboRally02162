import React from "react";
import { registerEffect } from "../effectRegistry";
import type { WallEffect } from "../../types/boardTypes";

/**
 * @author William Pii Jæger
 */

export default function Wall({ effect }: { effect: WallEffect }) {
  // Rotation degrees for each direction
  const rotationMap: Record<string, number> = {
    N: 0,
    E: 90,
    S: 180,
    W: 270,
  };
  return (
    <>
      <>
        {effect.walls.map((direction) => (
          <img
            key={direction}
            src={`${process.env.PUBLIC_URL}/boardelements/walls/wall-n.png`}
            alt={`wall ${direction}`}
            className={`wall wall-${direction.toLowerCase()}`}
            style={{
              transform: `rotate(${rotationMap[direction] || 0}deg)`
            }}
          />
        ))}
      </>

    </>
  );
}
registerEffect("walldto", Wall);