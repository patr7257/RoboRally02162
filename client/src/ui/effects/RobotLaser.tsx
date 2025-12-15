import React, { useEffect, useState, useRef } from "react";
import { registerEffect } from "../effectRegistry";
import type { RobotLaserEffect, Direction, Robot, Tile } from "../../types/boardTypes";
import { subscribe } from "../../utils/ws";
import { calculateLaserBeamLength } from "../../utils/boardUtils";

/**
 * RobotLaser effect visuals
 * Shoots laser from a robot's position in the direction it's facing.
 * Calculates whether another robot is hit by checking line of sight.
 * @author Patrick Røbel
 */

type RobotLaserProps = {
  effect: RobotLaserEffect & { 
    robotId: number;
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
 * Checks if there's another robot (not the shooter) in the laser's line of sight.
 * Traces the laser path from the shooting robot's position until it hits another robot, wall, antenna, or goes off-board.
 * @author Patrick Røbel
 */
function hasRobotInLineOfSight(
  startX: number,
  startY: number,
  direction: Direction,
  shootingRobotId: number,
  board: { tiles: Tile[][]; width: number; height: number },
  robots: Robot[]
): boolean {
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

    // Check if any OTHER robot is at this position (exclude the shooting robot)
    if (robots.some(robot => robot.x === currentX && robot.y === currentY && robot.id !== shootingRobotId)) {
      return true;
    }

    previousX = currentX;
    previousY = currentY;
  }
}

export default function RobotLaser({ effect }: RobotLaserProps) {
  const { direction, robotId, x, y, board, robots } = effect;
  
  // Calculate if there's another robot in line of sight (excluding the shooting robot)
  const hasTarget = x !== undefined && y !== undefined && board && robots
    ? hasRobotInLineOfSight(x, y, direction, robotId, board, robots)
    : false;

  const beamLength = calculateLaserBeamLength(x, y, direction, board, 'robot');
  
  const [isFiring, setIsFiring] = useState(false);
  const firingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastFiringTimeRef = useRef(0);
  
  // Get robot color from ROBOT_COLORS based on robotId
  const getRobotLaserColorRGB = (id: number): string => {
    const colors = [
      "59, 130, 246",    // blue
      "255, 255, 255",   // white
      "34, 197, 94",     // green
      "250, 204, 21",    // yellow
      "239, 68, 68",     // red
      "168, 85, 247",    // purple
    ];
    return colors[(id - 1) % colors.length];
  };
  
  const robotLaserColor = getRobotLaserColorRGB(robotId);

  // Listen for WebSocket messages to trigger laser firing
  useEffect(() => {
    const handleMessage = (messageStr: string) => {
      try {
        const message = JSON.parse(messageStr);

        if (
          message.type === "tileAnimation" &&
          message.payload &&
          message.payload.effectKind === "robot_laser"
        ) {
          const { x: tileX, y: tileY } = message.payload;
          if (x === tileX && y === tileY) {
            triggerFiring();
          }
        }
      } catch (e) {
      }
    };

    const unsubscribe = subscribe(handleMessage);

    return () => {
      unsubscribe();
      if (firingTimeoutRef.current) {
        clearTimeout(firingTimeoutRef.current);
      }
    };
  }, [x, y, robotId]);
  
  /*
  // TESTING ONLY - see all lasers firing every 3 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      triggerFiring();
    }, 3000);
    return () => clearInterval(interval);
  }, []);
  */

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
    }, 600);
  };

  const firingClass = isFiring ? "laser-firing" : "";

  return (
    <div 
      className={`robot-laser ${firingClass}`}
      style={{ "--current-robot-laser-color": robotLaserColor } as React.CSSProperties}>
      {isFiring && (
        <div
          className="robot-laser-beam"
          data-direction={direction}
          style={{
            ...(direction === 'N' || direction === 'S' 
              ? { height: `${beamLength * 100}%` }
              : { width: `${beamLength * 100}%` }
            ),
          }}
        />
      )}
    </div>
  );
}

registerEffect("robot_laser", RobotLaser);
