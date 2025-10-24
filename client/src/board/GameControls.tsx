import React from "react";
import { MoveType } from "../types/boardTypes";
import { MoveSelector } from "./MoveSelector";

/*
Author(s): Bjarke, Asger
*/

interface GameControlsProps {
  selectedMoves: (MoveType | null)[];
  onSubmitMove: (moves: MoveType[]) => void;
  onSelectMove: (moves: (MoveType | null)[]) => void;
  hand: MoveType[];
}

export const GameControls: React.FC<GameControlsProps> = ({
  selectedMoves,
  onSubmitMove,
  onSelectMove,
  hand,
}) => (
  <div className="controls">
    <MoveSelector
      moves={hand}
      selectedMoves={selectedMoves}
      onChange={onSelectMove}
    />
    <button
      onClick={() => onSubmitMove(selectedMoves.filter((m): m is MoveType => m !== null))}
      disabled={selectedMoves.some((m) => m === null)}
    >
      Make Move
    </button>
  </div>
);
