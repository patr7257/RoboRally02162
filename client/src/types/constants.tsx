/** 
* @author Bjarke Søderhamn Petersen
* @author Asger Allin Jensen
* @author Patrick Røbel
* @author Lizette Bloch Dahl Nikolajsen
*/

import { Direction, MoveType} from "./boardTypes";

export const ROBOT_COLORS = [
  "#3b82f6", // blue
  "#ffffff", // white
  "#22c55e", // green
  "#facc15", // yellow
  "#ef4444", // red
  "#a855f7", // purple
];

export const ROBOT_IMAGES = [
    "/boardelements/robots/blueRobot.png",
    "/boardelements/robots/whiteRobot.png",
    "/boardelements/robots/greenRobot.png",
    "/boardelements/robots/yellowRobot.png",
    "/boardelements/robots/redRobot.png",
    "/boardelements/robots/purpleRobot.png",
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