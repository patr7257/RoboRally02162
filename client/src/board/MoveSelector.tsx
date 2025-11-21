import React, { useState } from "react";
import { motion } from "framer-motion";
import { MoveType } from "../types/boardTypes";

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Bloch Dahl Nikolajsen
*/

interface MoveSelectorProps {
  moves: MoveType[];
  selectedMoves: (MoveType | null)[];
  onChange: (selectedMoves: (MoveType | null)[]) => void;
  onSubmitMove: (moves: MoveType[]) => void;
  hasEmptySlots: boolean;
}
/**
* @author Lizette Bloch Dahl Nikolajsen
*/
const CardVisual = ({ move }: { move: MoveType }) => (
  <>
    <div className="moveCardHeader">
      {move.replace("ROTATE", "").replace("MOVE", "MOVE ")}
    </div>
    <div className="moveCardBody">
      {move === "MOVE1" && (
        <svg viewBox="0 0 100 100">
          <path d="M50 90 L50 20" />
          <path d="M50 20 L40 35" />
          <path d="M50 20 L60 35" />
        </svg>
      )}
      {move === "MOVE2" && (
        <svg viewBox="0 0 100 100">
          <path d="M35 90 L35 20" />
          <path d="M35 20 L25 35" />
          <path d="M35 20 L45 35" />
          <path d="M65 90 L65 20" />
          <path d="M65 20 L55 35" />
          <path d="M65 20 L75 35" />
        </svg>
      )}
      {move === "MOVE3" && (
        <svg viewBox="0 0 100 100">
          <path d="M25 90 L25 20" />
          <path d="M25 20 L15 35" />
          <path d="M25 20 L35 35" />
          <path d="M50 90 L50 20" />
          <path d="M50 20 L40 35" />
          <path d="M50 20 L60 35" />
          <path d="M75 90 L75 20" />
          <path d="M75 20 L65 35" />
          <path d="M75 20 L85 35" />
        </svg>
      )}
      {move === "MOVEBACK" && (
        <svg viewBox="0 0 100 100">
          <path d="M50 20 L50 80" />
          <path d="M50 80 L40 65" />
          <path d="M50 80 L60 65" />
        </svg>
      )}
      {move === "ROTATELEFT" && (
        <svg viewBox="0 0 100 100">
          <path d="M65 80 L65 35 L30 35" />
          <path d="M30 35 L40 25" />
          <path d="M30 35 L40 45" />
        </svg>
      )}
      {move === "ROTATERIGHT" && (
        <svg viewBox="0 0 100 100">
          <path d="M35 80 L35 35 L70 35" />
          <path d="M70 35 L60 25" />
          <path d="M70 35 L60 45" />
        </svg>
      )}
      {move === "UTURN" && (
        <svg viewBox="0 0 100 100">
          <path d="M65 85 L65 45 Q65 25 50 25 Q35 25 35 45 L35 85" />
          <path d="M35 85 L30 75" />
          <path d="M35 85 L40 75" />
        </svg>
      )}
    </div>
  </>
);

/**
* @author Asger Allin Jensen
*/
const MoveCard = ({
  move,
  onDragStart,
  isDragging,
  index,
}: {
  move: MoveType;
  onDragStart: () => void;
  isDragging: boolean;
  index: number;
}) => (
  <motion.div
    draggable
    onDragStart={onDragStart}
    className={`moveCard card-index-${index + 1}`}
    whileTap={{ scale: 0.95 }}
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    style={{
      opacity: isDragging ? 0.5 : 1,
    }}
  >
    <CardVisual move={move} />
  </motion.div>
);

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
*/
const DropSlot = ({
  index,
  move,
  onDrop,
  isOver,
  onDragStart,
  onDragEnd,
  isDragging,
}: {
  index: number;
  move: MoveType | null;
  onDrop: () => void;
  isOver: boolean;
  onDragStart: () => void;
  onDragEnd: () => void;
  isDragging: boolean;
}) => (
  <motion.div
    onDragOver={(e) => e.preventDefault()}
    onDrop={onDrop}
    className={`dropSlot ${isOver ? 'dragOver' : ''} ${move ? 'filled' : 'empty'} ${isDragging ? 'is-drag-source' : ''}`}
    whileHover={{ scale: 1.02 }}
  >
    {move ? (
      <div
        className="moveCard"
        draggable
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
        style={{ cursor: 'grab' }}
      >
        <CardVisual move={move} />
      </div>
    ) : (
      <span className="slotPlaceholder">Slot {index + 1}</span>
    )}
  </motion.div>
);

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
*/
export const MoveSelector: React.FC<MoveSelectorProps> = ({
  moves,
  selectedMoves,
  onChange,
  onSubmitMove,
  hasEmptySlots,
}) => {
  const [draggedMove, setDraggedMove] = useState<MoveType | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const [dragSourceIndex, setDragSourceIndex] = useState<number | null>(null);

  const getAvailableMoves = () => {
    const moveCounts = new Map<MoveType, number>();

    moves.forEach(move => {
      moveCounts.set(move, (moveCounts.get(move) || 0) + 1);
    });

    selectedMoves.forEach(move => {
      if (move !== null) {
        moveCounts.set(move, (moveCounts.get(move) || 0) - 1);
      }
    });

    const available: MoveType[] = [];
    moveCounts.forEach((count, move) => {
      for (let i = 0; i < count; i++) {
        available.push(move);
      }
    });

    return available;
  };

  const availableMoves = getAvailableMoves();

  /**
  * @author Asger Allin Jensen
  * @author Bjarke Søderhamn Petersen
  */
  const handleDragStart = (move: MoveType) => {
    setDraggedMove(move);
    setDragSourceIndex(null);
  };

  /**
  * @author Asger Allin Jensen
  * @author Bjarke Søderhamn Petersen
  */
  const handleSlotDragStart = (move: MoveType, index: number) => {
    setDraggedMove(move);
    setDragSourceIndex(index);
  };

  /**
  * @author Asger Allin Jensen
  * @author Bjarke Søderhamn Petersen
  */
  const handleDrop = (index: number) => {
    if (draggedMove) {
      const updated = [...selectedMoves];
      
      if (dragSourceIndex !== null) {
        updated[dragSourceIndex] = null;
      }
      
      updated[index] = draggedMove;
      onChange(updated);
      setDraggedMove(null);
      setDragOverIndex(null);
      setDragSourceIndex(null);  
    }
  };

  /**
  * @author Asger Allin Jensen
  * @author Bjarke Søderhamn Petersen
  */
  const handleDropToHand = () => {
    if (draggedMove && dragSourceIndex !== null) {
      const updated = [...selectedMoves];
      updated[dragSourceIndex] = null;
      onChange(updated);
    }
    setDraggedMove(null);
    setDragSourceIndex(null);
  };

  /**
  * @author Bjarke Søderhamn Petersen
  */
  const handleDragEnd = () => {
    setDraggedMove(null);
    setDragSourceIndex(null);
  };

  return (
    <div className="moveSelectorContainer">
      <div className="programSlotsSection">
        <h3 className="sectionTitle">
          YOUR PROGRAM ({selectedMoves.filter(m => m !== null).length}/5)
        </h3>
        <div className="controls-layout">
          <div className="programSlotsGrid">
            {selectedMoves.map((move, index) => (
              <DropSlot
                key={index}
                index={index}
                move={move}
                onDrop={() => handleDrop(index)}
                isOver={dragOverIndex === index}
                onDragStart={() => move && handleSlotDragStart(move, index)}
                onDragEnd={handleDragEnd}
                isDragging={dragSourceIndex === index}
              />
            ))}
          </div>
        </div>
        <button
          className="make-move-button"
          onClick={() => onSubmitMove(selectedMoves.filter((m): m is MoveType => m !== null))}
          disabled={hasEmptySlots}
        >
          Make Move
        </button>
      </div>
      <div
        className="availableMovesSection"
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDropToHand}
      >
        <h3 className="sectionTitle">AVAILABLE CARDS</h3>
        <div className={`availableMovesGrid ${availableMoves.length === 0 ? 'emptyGrid' : ''}`}>
          {availableMoves.length > 0 ? (
            availableMoves.map((move, idx) => (
              <MoveCard
                key={`${move}-${idx}`}
                move={move}
                onDragStart={() => handleDragStart(move)}
                isDragging={draggedMove === move}
                index={idx}
              />
            ))
          ) : (
            <p className="noCardsMessage">All cards selected</p>
          )}
        </div>
      </div>
    </div>
  );
};