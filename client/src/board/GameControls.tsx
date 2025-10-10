import React from "react";
import { MoveType } from "../types/boardTypes";
import { MoveSelector } from "./MoveSelector";

/*
Author(s): Asger
*/

interface GameControlsProps {
  selectedMove: MoveType | null;
  onSubmitMove: () => void;
  onStartRound: () => void;
}

export const GameControls: React.FC<GameControlsProps> = ({
  selectedMove,
  onSubmitMove,
  onStartRound,
}) => (
  <div className="controls">
    <MoveSelector
      selectedMove={selectedMove}
      onSelectMove={(move) => {
        /* This will be handled by parent */
      }}
    />
    <button onClick={onSubmitMove} disabled={!selectedMove}>
      Make Move
    </button>
    <button onClick={onStartRound}>Start Round</button>
  </div>
);