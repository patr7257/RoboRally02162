import React from "react";
import { registerEffect } from "../effectRegistry";
import type { CheckpointEffect } from "../../types/boardTypes";

// Author(s) William, Weihao

function Checkpoint({ effect }: { effect: CheckpointEffect }) {
  return <div className="checkpoint-badge" aria-label="checkpoint">{effect.number}</div>;
}

registerEffect("checkpoint", Checkpoint);
export {};
