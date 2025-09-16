import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function Board() {
  const navigate = useNavigate();
  // number of squares per side (user input)
  const [sideInput, setSideInput] = useState(5);

  // coerce to integer >= 1
  const n = Math.max(1, Math.floor(Number(sideInput) || 0));

  // grid style returned by helper
  const gridStyle = calculateSize(n, n);

  return (
    <div className="board-root">
      

      <div className="navigation">
        <h1>Board Scene</h1>
        <button onClick={() => navigate('/')}>Go to homepage</button>
      </div>

      <div className="controls">
        <label htmlFor="sides">Squares per side:</label>
        <input
          id="sides"
          className="inputNumber"
          type="number"
          min="1"
          value={sideInput}
          onChange={(e) => setSideInput(e.target.value)}
        />

        <div style={{ marginLeft: 'auto' }}>
          Total: <strong>{n * n}</strong>
        </div>
      </div>

      <div className="gameboard" style={gridStyle}>
        {Array.from({ length: n * n }).map((_, idx) => (
          <div key={idx} className="cell">{idx + 1}</div>
        ))}
      </div>
    </div>
  );
}


function calculateSize(sizeX, sizeY) {
  return {
    gridTemplateColumns: `repeat(${Math.max(1, sizeX)}, 1fr)`,
  };
}
