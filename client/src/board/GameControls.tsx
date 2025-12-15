import React from "react";
import { MoveType } from "../types/boardTypes";
import { MoveSelector } from "./MoveSelector";
import "./board.css"

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Nikolajsen
* @author Patrick Røbel
* @author William Pii Jæger
* @author Benjamin Benyo Endahl Hansen
* @author Karl Johannes Agerbo
*/

interface GameControlsProps {
  selectedMoves: (MoveType | null)[];
  onSubmitMove: (moves: MoveType[]) => void;
  onSelectMove: (moves: (MoveType | null)[]) => void;
  hand: MoveType[];
  discard: MoveType[];
  isDemoMode: boolean;
  onForceStartRound: () => void;
}

/**
 *  @author Asger Allin Jensen
 *  @author Bjarke Søderhamn Petersen
 */ 
export const GameControls: React.FC<GameControlsProps> = ({
  selectedMoves,
  onSubmitMove,
  onSelectMove,
  hand,
  discard,
  isDemoMode,
  onForceStartRound,
}) => (
  <div className="controls">

    <details className="discard-dropdown">
      <summary>Discard Pile ({discard.length})</summary>
      <ul>
        {discard.map((card, index) => (
          <li key={index}>{card}</li>
        ))}
      </ul>
    </details>

    <div className="move-controls-row">
      <MoveSelector
        moves={hand}
        selectedMoves={selectedMoves}
        onChange={onSelectMove}
        onSubmitMove={onSubmitMove}
        hasEmptySlots={selectedMoves.some((m) => m === null)}
      />

      {isDemoMode && (
        <button
          type="button"
          className="force-start-round-btn"
          onClick={onForceStartRound}
        >
          Force Start Round
        </button>
      )}
    </div>
  </div>
);
