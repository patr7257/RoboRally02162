import React, { useEffect, useState, useRef } from "react";
import { registerEffect } from "../effectRegistry";
import { Rotation } from "../../types/boardTypes";
import { subscribe } from "../../utils/ws";

type GearEffect = {
  rotation: Rotation;
  x?: number;
  y?: number;
};

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
function Gear({ effect }: { effect: GearEffect }) {
  const [isAnimating, setIsAnimating] = useState(false);
  const animationTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastAnimationTimeRef = useRef(0);

  const gearImage =
    effect.rotation === "LEFT"
      ? `${process.env.PUBLIC_URL}/boardelements/Gear/GearLeft.svg`
      : effect.rotation === "RIGHT"
      ? `${process.env.PUBLIC_URL}/boardelements/Gear/GearRight.svg`
      : null;

  const dirClass =
    effect.rotation === "LEFT"
      ? "dir-ccw"
      : effect.rotation === "RIGHT"
      ? "dir-cw"
      : "";

  useEffect(() => {
    const handleMessage = (messageStr: string) => {
      try {
        const message = JSON.parse(messageStr);

        if (
          message.type === "tileAnimation" &&
          message.payload &&
          message.payload.effectKind === "geardto"
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

    const activeClass = isAnimating ? "activated" : "";

    return (
      <div
        className={`tile-effect gear ${dirClass} ${activeClass}`}
        style={gearImage ? { backgroundImage: `url(${gearImage})` } : undefined}
        aria-label={`gear-${String(effect.rotation).toLowerCase()}`}
      />
    );
  }

registerEffect("geardto", Gear);