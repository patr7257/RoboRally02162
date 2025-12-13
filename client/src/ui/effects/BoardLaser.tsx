import React, { useEffect, useState, useRef } from "react";
import { registerEffect } from "../effectRegistry";
import { Direction } from "../../types/boardTypes";
import { subscribe } from "../../utils/ws";

type BoardLaserEffect = {
  direction: Direction;
  power: number;
  x?: number;
  y?: number;
  board?: { width: number; height: number };
};

/**
 * @author Patrick Røbel
 */
function BoardLaser({ effect }: { effect: BoardLaserEffect }) {
  const [isAnimating, setIsAnimating] = useState(false);
  const animationTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastAnimationTimeRef = useRef(0);
  const { direction, power, x, y, board } = effect;
  const laserImage = `${process.env.PUBLIC_URL}/boardelements/lasers/laser-${direction.toLowerCase()}.png`;

  const getBeamLength = (): number => {
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
    return tilesToEdge + 0.65;
  };

  const beamLength = getBeamLength();

  // Position lasers against the wall (opposite of firing direction)
  const getPositionStyle = () => {
    switch (direction) {
      case 'N':
        return { fixedAxis: 'top', fixedValue: '85%', spreadAxis: 'left' };
      case 'S':
        return { fixedAxis: 'top', fixedValue: '15%', spreadAxis: 'left' };
      case 'E':
        return { fixedAxis: 'left', fixedValue: '15%', spreadAxis: 'top' };
      case 'W':
        return { fixedAxis: 'left', fixedValue: '85%', spreadAxis: 'top' };
      default:
        return { fixedAxis: 'top', fixedValue: '50%', spreadAxis: 'left' };
    }
  };

  // Calculate laser positions based on power
  const getLaserPositions = () => {
    if (power === 1) {
      return [50];
    } else if (power === 2) {
      return [33.33, 66.67];
    } else if (power === 3) {
      return [25, 50, 75];
    }
    return [50];
  };

  const positions = getLaserPositions();
  const positionStyle = getPositionStyle();

  useEffect(() => {
    const handleMessage = (messageStr: string) => {
      try {
        const message = JSON.parse(messageStr);

        if (
          message.type === "tileAnimation" &&
          message.payload &&
          message.payload.effectKind === "board_laser"
        ) {
          const { x, y } = message.payload;

          if (effect.x === x && effect.y === y) {
            triggerAnimation();
          }
        }
      } catch (e) {
      }
    };

    const unsubscribe = subscribe(handleMessage);

    return () => {
      unsubscribe();
      if (animationTimeoutRef.current) {
        clearTimeout(animationTimeoutRef.current);
      }
    };
  }, [effect.x, effect.y]);

  const triggerAnimation = () => {
    const now = Date.now();

    if (isAnimating) return;

    if (now - lastAnimationTimeRef.current < 1000) return;

    lastAnimationTimeRef.current = now;

    if (animationTimeoutRef.current) {
      clearTimeout(animationTimeoutRef.current);
    }

    setIsAnimating(true);

    animationTimeoutRef.current = setTimeout(() => {
      setIsAnimating(false);
    }, 600);
  };

  const firingClass = isAnimating ? "laser-firing" : "";
  
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
            transform: "translate(-50%, -50%)",
            transformOrigin: "center center",
          }}
        />
      ))}

      {/* Laser beams - only visible when firing */}
      {isAnimating &&
        positions.map((pos, index) => (
          <div
            key={`beam-${index}`}
            className="laser-beam"
            data-direction={direction}
            style={{
              [positionStyle.spreadAxis]: `${pos}%`,
              ...(direction === 'N' || direction === 'S' 
                ? { height: `${beamLength * 100}%` }
                : { width: `${beamLength * 100}%` }
              ),
            }}
          />
        ))}
    </div>
  );
}

registerEffect("board_laser", BoardLaser);