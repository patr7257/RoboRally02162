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
 * @author William Pii Jæger *
 */
export const calculateBoardSize = (
  boardWidth: number,
  boardHeight: number,
  containerElement?: HTMLElement | null
): { tileSize: number; boardWidth: number; boardHeight: number } => {
  let availableWidth: number;
  let availableHeight: number;

  if (containerElement) {

    const rect = containerElement.getBoundingClientRect();
    availableWidth = rect.width;
    availableHeight = rect.height;
  } else {

    availableWidth = window.innerWidth;
    availableHeight = window.innerHeight;
  }

  const tileSizeByWidth = (availableWidth / boardWidth)*0.95;
  const tileSizeByHeight = (availableHeight / boardHeight)*0.95;
  const tileSize = Math.min(tileSizeByWidth, tileSizeByHeight);

  return {
    tileSize: Math.floor(tileSize),
    boardWidth: boardWidth * Math.floor(tileSize),
    boardHeight: boardHeight * Math.floor(tileSize),
  };
};

/**
 * @author Patrick Røbel
 */
export const calculateLaserBeamLength = (
  x: number | undefined,
  y: number | undefined,
  direction: Direction,
  board: { width: number; height: number } | undefined,
  laserType: 'board' | 'robot'
): number => {
  if (x === undefined || y === undefined || !board) {
    return 10;
  }

  let tilesToEdge = 0;
  switch (direction) {
    case 'N':
      tilesToEdge = y;
      break;
    case 'S':
      tilesToEdge = board.height - y - 1;
      break;
    case 'E':
      tilesToEdge = board.width - x - 1;
      break;
    case 'W':
      tilesToEdge = x;
      break;
  }

  const extraTiles = laserType === 'board' ? 0.65 : 0.5;
  return tilesToEdge + extraTiles;
};