import React, { useEffect, useState } from "react";
import Dropdown from "react-bootstrap/Dropdown";
import "bootstrap/dist/css/bootstrap.min.css";
import { MoveType } from "../types/boardTypes";

/*
Author(s): Bjarke, Asger, Niklas
*/

interface MoveSelectorProps {
  moves: MoveType[];
  count?: number;
  selectedMoves: (MoveType | null)[];
  onChange: (selectedMoves: (MoveType | null)[]) => void;
}

export const MoveSelector: React.FC<MoveSelectorProps> = ({
  moves,
  count = 5,
  selectedMoves,
  onChange,
}) => {
  const [localSelected, setLocalSelected] = useState<(MoveType | null)[]>(selectedMoves);

  useEffect(() => {
    setLocalSelected(selectedMoves);
  }, [selectedMoves]);

  const handleSelect = (index: number, value: MoveType) => {
    const updated = [...localSelected];
    updated[index] = value;
    setLocalSelected(updated);
    onChange(updated);
  };

  const getAvailableMoves = (index: number) => {
    const remaining = [...moves];
    localSelected.forEach((m, i) => {
      if (i !== index && m !== null) {
        const removeIndex = remaining.indexOf(m);
        if (removeIndex !== -1) remaining.splice(removeIndex, 1);
      }
    });
    return remaining;
  };

  return (
    <div style={{ display: "flex", gap: "10px" }}>
      {Array.from({ length: count }).map((_, i) => (
        <Dropdown key={i}>
          <Dropdown.Toggle variant="success">
            {localSelected[i] || "Choose Move"}
          </Dropdown.Toggle>
          <Dropdown.Menu>
            {getAvailableMoves(i).map((move, idx) => (
              <Dropdown.Item key={`${move}-${idx}`} onClick={() => handleSelect(i, move)}>
                {move}
              </Dropdown.Item>
            ))}
          </Dropdown.Menu>
        </Dropdown>
      ))}
    </div>
  );
};
