import React from "react";

/**
 * @author Asger Allin Jensen
 */

interface ReactionPopUpProps {
  onClose: () => void;
  reactionKind: string; // e.g., "WEASEL" | "SANDBOX" | "SPEED"
  options: string[]; // reaction options to render as buttons
  onSelect: (option: string) => void; // callback when user picks
}

const ReactionPopUp: React.FC<ReactionPopUpProps> = ({ onClose, reactionKind, options, onSelect }) => {
    return (
        <div className="overlay" onClick={onClose}>
          <div className="popUp" onClick={(e) => e.stopPropagation()}>
            <button className="closeButton" onClick={onClose}>x</button>
            <div className="reactionName">
              <h1>{reactionKind}</h1>
            </div>
            <div className="content">
              {options.map((option) => (
                <button
                  key={option}
                  onClick={() => onSelect(option)}
                  style={{
                    margin: "5px",
                    padding: "10px 20px",
                    borderRadius: "5px",
                    border: "1px solid #333",
                    cursor: "pointer",
                    backgroundColor:  "rgba(250, 204, 21, 0.15)",
                  }}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>
        </div>
  );
};

export default ReactionPopUp;
