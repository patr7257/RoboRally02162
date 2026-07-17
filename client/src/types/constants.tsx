/** 
* @author Bjarke Søderhamn Petersen
* @author Asger Allin Jensen
* @author Patrick Røbel
* @author Lizette Bloch Dahl Nikolajsen
*/

import { Direction, MoveType } from "./boardTypes";

export const ROBOT_COLORS = [
    "#3b82f6", // blue
    "#ffffff", // white
    "#22c55e", // green
    "#facc15", // yellow
    "#ef4444", // red
    "#a855f7", // purple
];

// PUBLIC_URL prefixes public assets so absolute paths resolve under the arcade
// iframe subfolder (homepage), not the site root.
const PUB = process.env.PUBLIC_URL || "";

export const ROBOT_IMAGES = [
    `${PUB}/boardelements/robots/blueRobot.png`,
    `${PUB}/boardelements/robots/whiteRobot.png`,
    `${PUB}/boardelements/robots/greenRobot.png`,
    `${PUB}/boardelements/robots/yellowRobot.png`,
    `${PUB}/boardelements/robots/redRobot.png`,
    `${PUB}/boardelements/robots/purpleRobot.png`,
];

export const DIRECTION_ARROWS: Record<Direction, string> = {
    N: "↑",
    E: "→",
    S: "↓",
    W: "←",
};

export const MOVE_OPTIONS: Array<{ value: MoveType; label: string }> = [
    { value: "MOVE1", label: "Move 1" },
    { value: "MOVE2", label: "Move 2" },
    { value: "MOVE3", label: "Move 3" },
    { value: "MOVEBACK", label: "Move Back" },
    { value: "ROTATERIGHT", label: "Rotate Right" },
    { value: "ROTATELEFT", label: "Rotate Left" },
    { value: "UTURN", label: "U-turn" },
];