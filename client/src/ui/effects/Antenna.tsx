import React from "react";
import { registerEffect } from "../effectRegistry";
import type { AntennaEffect } from "../../types/boardTypes";

/*
* @author Weihao Mo
*/
const ANTENNA_IMAGE = "/antenna-n.jpg";

const ROTATION_ANGLES: Record<string, number> = {
    N: 0,
    E: 90,
    S: 180,
    W: 270
};

function Antenna({ effect }: { effect: AntennaEffect }) {
    const rotation = ROTATION_ANGLES[effect.direction];

    return(
        <div
         className = "antenna"
         aria-label = {`antenna ${effect.direction.toLowerCase()}`}
        >
            <img
                src={ANTENNA_IMAGE}
                alt={`Antenna facing ${effect.direction}`}
                className="antenna-image"
                style={{
                    transform: `rotate(${rotation}deg)`,
                    transformOrigin: 'center center'
                }}
            />
        </div>
    );
}

registerEffect("antenna", Antenna);