/*
Author(s): Bjarke, Patrick, Niklas
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

  const leaveLobby = async () => {
    setError("");
    try
     {
      const response = await fetch(API_BASE_URL+"/api/lobby/leave", {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify({lobbyID: lobbyId, username:usernameInput}),
      });
     } catch (err) {
      console.error("leave lobby error:",err);
      setError("Network error. Try Again.");
     }
  }

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

        <button className="big-button" onClick={() => {leaveLobby(); navigate("/lobbyScene")}}>
          Leave Lobby
        </button>
      </div>
    </Layout>
  );
}
