import React from "react";
import { registerEffect } from "../effectRegistry";
import type { RebootTokenEffect } from "../../types/boardTypes";

/*
* @author Weihao Mo
*/

const getRotationDegrees = (direction: string): number => {
  switch (direction) {
    case "N": return 0;
    case "E": return 90;
    case "S": return 180;
    case "W": return 270;
    default: return 0;
  }
};

export default function RebootToken({ effect }: { effect: RebootTokenEffect }) {
  return (
    <div
      className="reboot-token"
      title={`Reboot Token (${effect.direction})`}
    >
      <img
        src={`${process.env.PUBLIC_URL}/rebootToken.png`}
        alt="Reboot Token"
        className="reboot-token-image"
        style={{
          transform: `rotate(${getRotationDegrees(effect.direction)}deg)`
        }}
      />
    </div>
  );
}

registerEffect("reboot_token", RebootToken);