/*
Author(s): Bjarke, Patrick
*/

import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState } from "react";
import { subscribe } from "./ws";

export default function LobbyCreation() {
  const navigate = useNavigate();
  const usernameInput: string | null = localStorage.getItem("username");
  const [lobbyId, setLobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [error, setError] = useState<string>("");

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
      const response = await fetch("http://localhost:8080/api/lobby/start", {
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
      <div
        style={{
          marginTop: "50px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: "12px",
        }}
      >
        <button className="big-button" onClick={() => startGame()}>
          Start Game
        </button>

        <button className="big-button" onClick={() => navigate("/lobbyScene")}>
          Go back to lobby menu
        </button>
      </div>
    </Layout>
  );
}
