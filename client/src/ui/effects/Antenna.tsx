import React from "react";
import { registerEffect } from "../effectRegistry";
import type { AntennaEffect } from "../../types/boardTypes";

/**
 * @author Weihao Mo
 */

const PUB = process.env.PUBLIC_URL || "";
const ANTENNA_IMAGES: Record<string, string> = {
    N: `${PUB}/boardelements/antennas/antenna-n.png`,
    E: `${PUB}/boardelements/antennas/antenna-e.png`,
    S: `${PUB}/boardelements/antennas/antenna-s.png`,
    W: `${PUB}/boardelements/antennas/antenna-w.png`
};

function Antenna({ effect }: { effect: AntennaEffect }) {
    const imgSrc = ANTENNA_IMAGES[effect.direction] || ANTENNA_IMAGES.N;
    return (
        <div
            className="antenna"
            aria-label={`antenna ${effect.direction.toLowerCase()}`}
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