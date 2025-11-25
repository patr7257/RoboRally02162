import React from "react";
import { registerEffect } from "../effectRegistry";
import type { PitsEffect } from "../../types/boardTypes";

/*
 * @author Weihao Mo
 */
const PITS_IMAGE = `${process.env.PUBLIC_URL}/pits.png`;

function Pits({ effect }: { effect: PitsEffect }) {
  const hasRobot = (effect as any).hasRobot as boolean | undefined;

  return (
    <div
      className={
        "tile-effect pits-effect" + (hasRobot ? " pits-effect--active" : "")
      }
    >
      <img
        src={PITS_IMAGE}
        alt="Pit"
        draggable={false}
        style={{
          width: "100%",
          height: "100%",
          objectFit: "cover",
          display: "block",
        }}
      />
    </div>
  );
}

registerEffect("pits", Pits);
export default Pits;
