import React from "react";
import { registerEffect } from "../effectRegistry";
import type { AntennaEffect } from "../../types/boardTypes";

/**
 * @author Weihao Mo
 */

const ANTENNA_IMAGES: Record<string, string> = {
    N: "/boardelements/antennas/antenna-n.png",
    E: "/boardelements/antennas/antenna-e.png",
    S: "/boardelements/antennas/antenna-s.png",
    W: "/boardelements/antennas/antenna-w.png"
};

function Antenna({ effect }: { effect: AntennaEffect }) {
    const imgSrc = ANTENNA_IMAGES[effect.direction] || ANTENNA_IMAGES.N;
    return(
        <div
         className = "antenna"
         aria-label = {`antenna ${effect.direction.toLowerCase()}`}
        >
            <img
                src={imgSrc}
                alt={`Antenna facing ${effect.direction}`}
                className="antenna-image"
            />
        </div>
    );
}

registerEffect("antenna", Antenna);