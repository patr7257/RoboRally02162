import React, { useEffect, useState, useRef } from "react";
import { registerEffect } from "../effectRegistry";
import type { BoardLaserEffect, Direction, Robot, Tile, TileEffect } from "../../types/boardTypes";

/**
 * BoardLaser effect visuals
 * Calculates whether a robot is in the laser's line of sight considering walls and antennas.
 * @author Patrick Røbel
 */

type BoardLaserProps = {
  effect: BoardLaserEffect & { 
    hasRobot?: boolean;
    x?: number;
    y?: number;
    board?: { tiles: Tile[][]; width: number; height: number };
    robots?: Robot[];
  };
};

/**
 * Checks if there's a wall between two adjacent tiles blocking the laser.
 * @author Patrick Røbel
 */
function hasWallBetween(
  board: { tiles: Tile[][]; width: number; height: number },
  fromX: number,
  fromY: number,
  toX: number,
  toY: number,
  direction: Direction
): boolean {
  // Check 'from' tile for wall in direction
  if (fromX >= 0 && fromX < board.width && fromY >= 0 && fromY < board.height) {
    const fromTile = board.tiles[fromX][fromY];
    const wallEffect = fromTile.effects.find(e => e.kind === "walldto") as { walls?: Direction[] } | undefined;
    if (wallEffect?.walls?.includes(direction)) {
      return true;
    }
  }

  // Check 'to' tile for wall in opposite direction
  if (toX >= 0 && toX < board.width && toY >= 0 && toY < board.height) {
    const toTile = board.tiles[toX][toY];
    const opposite: Direction = direction === "N" ? "S" : direction === "S" ? "N" : direction === "E" ? "W" : "E";
    const wallEffect = toTile.effects.find(e => e.kind === "walldto") as { walls?: Direction[] } | undefined;
    if (wallEffect?.walls?.includes(opposite)) {
      return true;
    }
  }

  return false;
}

/**
 * Checks if a tile has an antenna (which blocks lasers).
 * @author Patrick Røbel
 */
function hasAntenna(tile: Tile): boolean {
  return tile.effects.some(e => e.kind === "antenna");
}

/**
 * Calculates the next coordinate in the given direction.
 * @author Patrick Røbel
 */
function nextCoord(x: number, y: number, direction: Direction): [number, number] {
  switch (direction) {
    case "N": return [x, y - 1];
    case "S": return [x, y + 1];
    case "E": return [x + 1, y];
    case "W": return [x - 1, y];
  }
}

/**
 * Checks if there's a robot in the laser's line of sight.
 * Traces the laser path until it hits a robot, wall, antenna, or goes off-board.
 * @author Patrick Røbel
 */
function hasRobotInLineOfSight(
  startX: number,
  startY: number,
  direction: Direction,
  board: { tiles: Tile[][]; width: number; height: number },
  robots: Robot[]
): boolean {
  // First check if robot is on the laser tile itself
  if (robots.some(robot => robot.x === startX && robot.y === startY)) {
    return true;
  }

  let currentX = startX;
  let currentY = startY;
  let previousX = currentX;
  let previousY = currentY;

  while (true) {
    [currentX, currentY] = nextCoord(currentX, currentY, direction);

    // Off-board
    if (currentX < 0 || currentX >= board.width || currentY < 0 || currentY >= board.height) {
      return false;
    }

    // Wall between previous and current position
    if (hasWallBetween(board, previousX, previousY, currentX, currentY, direction)) {
      return false;
    }

    // Antenna blocks laser
    const currentTile = board.tiles[currentX][currentY];
    if (hasAntenna(currentTile)) {
      return false;
    }

    // Check if any robot is at this position
    if (robots.some(robot => robot.x === currentX && robot.y === currentY)) {
      return true;
    }

    previousX = currentX;
    previousY = currentY;
  }
}

export default function BoardLaser({ effect }: BoardLaserProps) {
  const { direction, power, x, y, board, robots } = effect;
  
  // Calculate if there's a robot in line of sight
  const hasTarget = x !== undefined && y !== undefined && board && robots
    ? hasRobotInLineOfSight(x, y, direction, board, robots)
    : false;
  const [isFiring, setIsFiring] = useState(false);
  const firingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastFiringTimeRef = useRef(0);
  
  // Get the correct laser image based on direction
  const laserImage = `${process.env.PUBLIC_URL}/boardelements/lasers/laser-${direction.toLowerCase()}.png`;
  
  // Position lasers against the wall (opposite of firing direction)
  const getPositionStyle = () => {
    switch (direction) {
      case 'N': // Fire north, place at south edge, spread horizontally
        return { fixedAxis: 'top', fixedValue: '85%', spreadAxis: 'left' };
      case 'S': // Fire south, place at north edge, spread horizontally
        return { fixedAxis: 'top', fixedValue: '15%', spreadAxis: 'left' };
      case 'E': // Fire east, place at west edge, spread vertically
        return { fixedAxis: 'left', fixedValue: '15%', spreadAxis: 'top' };
      case 'W': // Fire west, place at east edge, spread vertically
        return { fixedAxis: 'left', fixedValue: '85%', spreadAxis: 'top' };
      default:
        return { fixedAxis: 'top', fixedValue: '50%', spreadAxis: 'left' };
    }
  };

  // Calculate triangle positions based on power
  const getTrianglePositions = () => {
    if (power === 1) {
      return [50]; // Center at 50%
    } else if (power === 2) {
      return [33.33, 66.67]; // At 1/3 and 2/3
    } else if (power === 3) {
      return [25, 50, 75]; // At 1/4, 2/4, 3/4
    }
    return [50]; // Default to center
  };

  const positions = getTrianglePositions();
  const positionStyle = getPositionStyle();

  // Listen for round execution to trigger laser firing
  useEffect(() => {
    const handleRoundExecuted = () => {
      if (hasTarget) {
        triggerFiring();
      }
    };

    window.addEventListener("roundExecuted", handleRoundExecuted);

    return () => {
      window.removeEventListener("roundExecuted", handleRoundExecuted);
      if (firingTimeoutRef.current) {
        clearTimeout(firingTimeoutRef.current);
      }
    };
  }, [hasTarget]);

  const triggerFiring = () => {
    const now = Date.now();

    if (isFiring) return;

    if (now - lastFiringTimeRef.current < 1000) return;

    lastFiringTimeRef.current = now;

    if (firingTimeoutRef.current) {
      clearTimeout(firingTimeoutRef.current);
    }

    setIsFiring(true);

    firingTimeoutRef.current = setTimeout(() => {
      setIsFiring(false);
    }, 800);
  };

  const firingClass = isFiring ? "laser-firing" : "";

  return (
    <div className={`board-laser ${firingClass}`}>
      {/* Laser emitter icons */}
      {positions.map((pos, index) => (
        <img 
          key={index}
          src={laserImage}
          alt={`laser emitter ${direction}`}
          className="laser-emitter"
          style={{
            [positionStyle.fixedAxis]: positionStyle.fixedValue,
            [positionStyle.spreadAxis]: `${pos}%`,
            transform: 'translate(-50%, -50%)',
            transformOrigin: 'center center'
          }}
        />
      ))}
      
      {/* Laser beams - only visible when firing */}
      {isFiring && positions.map((pos, index) => (
        <div
          key={`beam-${index}`}
          className="laser-beam"
          data-direction={direction}
          style={{
            [positionStyle.spreadAxis]: `${pos}%`,
          }}
        />
      ))}
    </div>
  );
}

registerEffect("board_laser", BoardLaser);
