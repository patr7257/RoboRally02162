import React from "react";
import Dropdown from "react-bootstrap/Dropdown";
import "bootstrap/dist/css/bootstrap.min.css";
import { MoveType } from "../types/boardTypes";
import { MOVE_OPTIONS } from "../types/boardTypes";

/*
Author(s): Asger
*/

interface MoveSelectorProps {
  selectedMove: MoveType | null;
  onSelectMove: (move: MoveType) => void;
}

export const MoveSelector: React.FC<MoveSelectorProps> = ({
  selectedMove,
  onSelectMove,
}) => (
  <Dropdown>
    <Dropdown.Toggle variant="success" id="dropdown-basic">
      {selectedMove
        ? MOVE_OPTIONS.find((m) => m.value === selectedMove)?.label
        : "Choose Move"}
    </Dropdown.Toggle>

    <Dropdown.Menu>
      {MOVE_OPTIONS.map(({ value, label }) => (
        <Dropdown.Item key={value} onClick={() => onSelectMove(value)}>
          {label}
        </Dropdown.Item>
      ))}
    </Dropdown.Menu>
  </Dropdown>
);
