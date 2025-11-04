import React from "react";
import { MoveType } from "../types/boardTypes";
import { MoveSelector } from "./MoveSelector";
import "./board.css"

/*
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Nikolajsen
* @author Patrick Røbel
* @author William Pii Jæger
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
    <div>
      <MoveSelector
        moves={hand}
        selectedMoves={selectedMoves}
        onChange={onSelectMove}
      />
    </div>

    <button
      className="metal-button"
      onClick={() => onSubmitMove(selectedMoves.filter((m): m is MoveType => m !== null))}
      disabled={selectedMoves.some((m) => m === null)}
    >
      Make Move
    </button>
  </div>
);
