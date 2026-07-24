import React, { useCallback, useEffect, useState, useRef } from "react";
import { registerEffect } from "../effectRegistry";
import type { RobotLaserEffect, Robot, Tile } from "../../types/boardTypes";
import { subscribe } from "../../utils/ws";
import { calculateLaserBeamLength } from "../../utils/boardUtils";

/**
 * RobotLaser effect visuals
 * Shoots laser from a robot's position in the direction it's facing.
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

export default function RobotLaser({ effect }: RobotLaserProps) {
  const { direction, robotId, x, y, board } = effect;

  const beamLength = calculateLaserBeamLength(x, y, direction, board, 'robot');

  const [isFiring, setIsFiring] = useState(false);
  const isFiringRef = useRef(false);
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

  const triggerFiring = useCallback(() => {
    const now = Date.now();

    if (isFiringRef.current) return;

    if (now - lastFiringTimeRef.current < 1000) return;

    lastFiringTimeRef.current = now;

    if (firingTimeoutRef.current) {
      clearTimeout(firingTimeoutRef.current);
    }

    isFiringRef.current = true;
    setIsFiring(true);

    firingTimeoutRef.current = setTimeout(() => {
      isFiringRef.current = false;
      setIsFiring(false);
    }, 600);
  }, []);

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
  }, [x, y, robotId, triggerFiring]);

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
