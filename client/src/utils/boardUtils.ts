import { Direction, Robot, DIRECTION_ARROWS, ROBOT_COLORS, ROBOT_IMAGES } from "../types/boardTypes"

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export const getFacingArrow = (facing: Direction): string =>
  DIRECTION_ARROWS[facing] || "●";

export const getRobotColor = (id: number): string =>
  ROBOT_COLORS[(id - 1) % ROBOT_COLORS.length];

export const getRobotImage = (id: number): string =>
  ROBOT_IMAGES[(id - 1) % ROBOT_IMAGES.length];

export const getRobotAtPosition = (
  robots: Robot[],
  x: number,
  y: number
): Robot | null => robots.find((robot) => robot.x === x && robot.y === y) || null;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export const calculateBoardSize = (
  boardWidth: number,
  boardHeight: number
): { tileSize: number; boardWidth: number; boardHeight: number } => {
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const availableWidth = viewportWidth * 0.385;
  const availableHeight = 0;

  const tileSizeByWidth = availableWidth / boardWidth;
  const tileSizeByHeight = availableHeight / boardHeight;
  const tileSize = Math.max(tileSizeByWidth, tileSizeByHeight);

  return {
    tileSize: Math.floor(tileSize),
    boardWidth: boardWidth * Math.floor(tileSize),
    boardHeight: boardHeight * Math.floor(tileSize),
  };
};