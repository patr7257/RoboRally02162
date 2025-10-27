/*
Author(s): Bjarke, Patrick, Niklas
*/

import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState } from "react";
import { subscribe } from "../utils/ws";
import { leaveLobby } from './LeaveLobby';
export default function LobbyCreation() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  const unsubscribe = subscribe((message: string) => {
    console.log("Received message:", message);

    try {
      const data = JSON.parse(message);
      console.log("Parsed data:", data);

      if (data.type === "game" && data.payload?.action === "start") {
        console.log("Game started !");
        navigate("/boardScene");
      }
    } catch {
      console.log("Raw text message:", message);
    }
  });

  const startGame = async () => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL+'/api/lobby/start', {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lobbyID: lobbyId }),
      });
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Try again.");
    }
  };

 

  return (
    <Layout>
      <h1>Lobby Creation</h1>
      <p>Your lobby ID is: {lobbyId}</p>
      <div className="lobby-actions">
        <button className="big-button" onClick={() => startGame()}>
          Start Game
        </button>

        <button className="big-button" onClick={async () =>{await leaveLobby(lobbyId,userID,setError); navigate("/lobbyJoinScene")}}>
          Back to lobbies
        </button>
        
        <button className="big-button" onClick={async () => { await leaveLobby(lobbyId,userID,setError); navigate("/lobbyScene"); }}>
          Leave Lobby (exit)
        </button>
      </div>
    </Layout>
  );
}
