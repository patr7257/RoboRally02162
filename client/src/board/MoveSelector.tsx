import React, { useState } from "react";
import { motion } from "framer-motion";
import { MoveType } from "../types/boardTypes";

/*
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Lizette Nikolajsen
* @author Patrick Røbel
* @author William Pii Jæger
*/

interface MoveSelectorProps {
  moves: MoveType[];
  selectedMoves: (MoveType | null)[];
  onChange: (selectedMoves: (MoveType | null)[]) => void;
}

const MoveCard = ({ move, onDragStart, isDragging }: { 
  move: MoveType; 
  onDragStart: () => void;
  isDragging: boolean;
}) => (
  <motion.div
    draggable
    onDragStart={onDragStart}
    className={`moveCard ${isDragging ? 'dragging' : ''}`}
    whileHover={{ scale: 1.05, y: -5 }}
    whileTap={{ scale: 0.95 }}
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
  >
    {move}
  </motion.div>
);

const DropSlot = ({ 
  index, 
  move, 
  onDrop, 
  onRemove,
  isOver 
}: { 
  index: number; 
  move: MoveType | null; 
  onDrop: () => void;
  onRemove: () => void;
  isOver: boolean;
}) => (
  <motion.div
    onDragOver={(e) => e.preventDefault()}
    onDrop={onDrop}
    className={`dropSlot ${isOver ? 'dragOver' : ''} ${move ? 'filled' : 'empty'}`}
    whileHover={{ scale: 1.02 }}
  >
    {move ? (
      <>
        <span>{move}</span>
        <button
          className="removeCardBtn"
          onClick={onRemove}
        >
          x
        </button>
      </>
    ) : (
      <span className="slotPlaceholder">Slot {index + 1}</span>
    )}
  </motion.div>
);

export const MoveSelector: React.FC<MoveSelectorProps> = ({
  moves,
  selectedMoves,
  onChange,
}) => {
  const [draggedMove, setDraggedMove] = useState<MoveType | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

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

  const handleDragStart = (move: MoveType) => {
    setDraggedMove(move);
  };

  const handleDrop = (index: number) => {
    if (draggedMove) {
      const updated = [...selectedMoves];
      updated[index] = draggedMove;
      onChange(updated);
      setDraggedMove(null);
      setDragOverIndex(null);
    }
  };

  const handleRemove = (index: number) => {
    const updated = [...selectedMoves];
    updated[index] = null;
    onChange(updated);
  };

  return (
    <div className="moveSelectorContainer">
      <div className="availableMovesSection">
        <h3 className="sectionTitle">AVAILABLE CARDS</h3>
        <div className={`availableMovesGrid ${availableMoves.length === 0 ? 'emptyGrid' : ''}`}>
          {availableMoves.length > 0 ? (
            availableMoves.map((move, idx) => (
              <MoveCard
                key={`${move}-${idx}`}
                move={move}
                onDragStart={() => handleDragStart(move)}
                isDragging={draggedMove === move}
              />
            ))
          ) : (
            <p className="noCardsMessage">All cards selected</p>
          )}
        </div>
      </div>

      <div className="programSlotsSection">
        <h3 className="sectionTitle">
          YOUR PROGRAM ({selectedMoves.filter(m => m !== null).length}/5)
        </h3>
        <div className="programSlotsGrid">
          {selectedMoves.map((move, index) => (
            <DropSlot
              key={index}
              index={index}
              move={move}
              onDrop={() => handleDrop(index)}
              onRemove={() => handleRemove(index)}
              isOver={dragOverIndex === index}
            />
          ))}
        </div>
      </div>
    </div>
  );
};
