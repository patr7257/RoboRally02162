/*
Author(s): Asger, Bjarke, Patrick
*/
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage } from "../ws";
import Layout from "../Layout";

import MoveDropDown from "./MoveDropDown";
import { getRobotColor, getFacingArrow } from "./boardUtils";
import BoardRenderer from "./BoardRenderer";
import ConfirmTurnComp from "./ConfirmTurnComp";
import StartRoundComp from "./StartRoundComp";

export default function Board() {
  const navigate = useNavigate();
  const [lobbyId, setLobbyId] = useState(localStorage.getItem("id") || "");
  const [gameData, setGameData] = useState(null);
  const [chosenItem, setChosenItem] = useState("");

  useEffect(() => {
    const unsubscribe = subscribe((message) => {
      try {
        const data = JSON.parse(message);
        console.log("Parsed data:", data);
        if (data.type === "game") {
          setGameData(data.payload);
        }
      } catch {
        console.log("Raw text message:", message);
      }
    });

    //Initialize game, contacting host
    sendMessage({
      lobbyID: lobbyId,
      payload: { type: "submitProgram", cards: [] },
    });

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, [lobbyId]);

  const movePayload = {
    lobbyID: lobbyId,
    payload: { type: "submitProgram", cards: [chosenItem] }, // <-- use chosenItem here
  };

  const startRoundPayload = {
    lobbyID: lobbyId,
    payload: { type: "startRound" },
  };

  return (
    <div className="board-root">
      <div className="navigation">
        <h1>Board Scene</h1>
        <button onClick={() => navigate("/")}>Go to homepage</button>
      </div>

      <BoardRenderer gameData={gameData} />

      <div className="controls">
        <div>
          <MoveDropDown onSelect={setChosenItem} />
        </div>

        <ConfirmTurnComp jsn={movePayload} />
        <StartRoundComp jsn={startRoundPayload} />
      </div>
    </div>
  );
}
