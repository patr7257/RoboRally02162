import { useState } from "react";
import Dropdown from "react-bootstrap/Dropdown";
import "bootstrap/dist/css/bootstrap.min.css";

function MoveDropDown({ onSelect }) {
    const [selected, setSelected] = useState("Choose Move");

    const handleSelect = (value) => {
        setSelected(value);   // update dropdown text
        onSelect(value);      // send chosen value to parent
    };

    return (
        <Dropdown>
            <Dropdown.Toggle variant="success" id="dropdown-basic">
                {selected}
            </Dropdown.Toggle>

            <Dropdown.Menu>
                <Dropdown.Item onClick={() => handleSelect("MOVE1")}>
                    Move 1
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleSelect("MOVE2")}>
                    Move 2
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleSelect("MOVE3")}>
                    Move 3
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleSelect("MOVEBACK")}>
                    Move Back
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleSelect("ROTATERIGHT")}>
                    Rotate Right
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleSelect("ROTATELEFT")}>
                    Rotate Left
                </Dropdown.Item>
                <Dropdown.Item onClick={() => handleSelect("UTURN")}>
                    U-turn
                </Dropdown.Item>

            </Dropdown.Menu>
        </Dropdown>
    );
}

export default MoveDropDown;
