import React from "react";
import { registerEffect } from "../effectRegistry";
import type { CheckpointEffect } from "../../types/boardTypes";

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
function Checkpoint({ effect }: { effect: CheckpointEffect }) {
  return <div className="checkpoint-badge" aria-label="checkpoint">{effect.number}</div>;
}

registerEffect("checkpoint", Checkpoint);
export {};
