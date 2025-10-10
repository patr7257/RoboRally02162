/*
Author(s): Bjarke, Asger, Patrick
*/


import { Direction, MoveType } from "./boardTypes";

export const ROBOT_COLORS = [
    "#ff4444",
    "#44ff44",
    "#4444ff",
    "#ffff44",
    "#ff44ff",
    "#44ffff",
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