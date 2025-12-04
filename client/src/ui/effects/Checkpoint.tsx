import React from "react";
import { registerEffect } from "../effectRegistry";
import type { CheckpointEffect } from "../../types/boardTypes";

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
function Checkpoint({ effect }: { effect: CheckpointEffect }) {
  return (
    <div 
      className="checkpoint-badge" 
      aria-label="checkpoint"
      style={{
        backgroundImage: `url(${process.env.PUBLIC_URL}/boardelements/checkpoints/checkpoint${effect.number}.png)`
      }}
    />
  );
}

registerEffect("checkpoint", Checkpoint);
export {};
