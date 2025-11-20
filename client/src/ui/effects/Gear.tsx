import React, { useEffect, useState, useRef } from "react";
import { registerEffect } from "../effectRegistry";
import { Rotation } from "../../types/boardTypes";

type GearEffect = {
  rotation: Rotation;
  hasRobot?: boolean;
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
      ? "Gear/GearLeft.svg"
      : effect.rotation === "RIGHT"
      ? "Gear/GearRight.svg"
      : null;

  const dirClass =
    effect.rotation === "LEFT"
      ? "dir-ccw"
      : effect.rotation === "RIGHT"
      ? "dir-cw"
      : "";

  useEffect(() => {
      const handleRoundExecuted = () => {
        if (effect.hasRobot) {
          triggerAnimation();
        }
      };

      window.addEventListener("roundExecuted", handleRoundExecuted);

      return () => {
        window.removeEventListener("roundExecuted", handleRoundExecuted);
        if (animationTimeoutRef.current) {
          clearTimeout(animationTimeoutRef.current);
        }
      };
    }, [effect.hasRobot]);

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