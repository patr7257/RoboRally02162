/**
Author(s): Bjarke, Patrick, Niklas
@author Asger Allin Jensen
*/

import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import { useEffect } from "react";
import React, { useState } from "react";
import { subscribe } from "../utils/ws";
import { leaveLobby } from './LeaveLobby';
export default function LobbyCreation() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [error, setError] = useState<string>("");
  const [isReady, setIsReady] = useState<boolean>(false); //local readiness for button
  const [playersReady, setPlayersReady] = useState<{ [username: string]: boolean }>({}); // total readiness

  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  useEffect(() => {
    const unsubscribe = subscribe((message: string) => {
      console.log("Received message:", message);

      try {
        const data = JSON.parse(message);
        console.log("Parsed data:", data);

        if (data.action === "Readiness") {
          let readyMap = data.payload;
          if (typeof readyMap === "string") {
            readyMap = JSON.parse(readyMap);
          }
          setPlayersReady(readyMap);
        }

        if (data.type === "game" && data.payload?.action === "start") {
          console.log("Game started!");
          navigate("/boardScene");
        } else if (data.type === "lobby" && data.action === "start_denied") {
          console.warn("Cannot start game:", data.payload.reason);
          setError(data.payload.reason);
        }
      } catch {
        console.log("Raw text message:", message);
      }
    });

    return () => {
      console.log("Unsubscribing from lobby websocket");
      unsubscribe?.();
    };
  }, [navigate]);



  useEffect(() => {
    const initializeReadiness = async () => {
      try {
        // Mark as not ready to trigger a readiness broadcast
        const response = await fetch(`${API_BASE_URL}/api/lobby/markNotReady`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            userID: userID,
            lobbyID: lobbyId,
          }),
        });

        if (response.status === 204) {
          console.log("Initial readiness state triggered");
        }
      } catch (err) {
        console.error("Error initializing readiness:", err);
      }
    };

    if (lobbyId && userID) {
      initializeReadiness();
    }
  }, [lobbyId, userID, API_BASE_URL]);

  const handleToggleReadiness = async () => {
    setError("");

    const endpoint = isReady ? "markNotReady" : "markReady";

    try {
      const response = await fetch(`${API_BASE_URL}/api/lobby/${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userID: userID,
          lobbyID: lobbyId,
        }),
      });

      if (response.status === 204) {
        setIsReady(!isReady);
        console.log(`Player marked as ${!isReady ? "READY" : "NOT READY"}`);
      } else {
        const text = await response.text();
        console.error("Operation failed:", text);
        setError(text);
      }
    } catch (err) {
      console.error("Network error:", err);
      setError("Network error. Try again.");
    }
  };

  const startGame = async () => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + '/api/lobby/start', {
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
      <div className="panel-container">
        <div className="lobby-panels-row">
          <div className="readiness-panel">
            <h2>Players</h2>
            <ul>
              {Object.entries(playersReady).map(([name, ready]) => (
                <li key={name} className={ready ? "ready" : "not-ready"}>
                  {name}: {ready ? "Ready" : "Not Ready"}
                </li>
              ))}
            </ul>
          </div>

          <div className="control-panel">
            <div className="lobby-id-display">
              <span className="lobby-id-label">LOBBY ID</span>
              <span className="lobby-id-value">{lobbyId}</span>
            </div>

            <button className="metal-button" onClick={startGame}>
              Start Game
            </button>

            <button className="metal-button" onClick={handleToggleReadiness}>
              {isReady ? "Not Ready" : "Ready"}
            </button>

            <button
              className="metal-button"
              onClick={async () => {
                await leaveLobby(lobbyId, userID, setError);
                navigate("/lobbyJoinScene");
              }}
            >
              Back to Lobbies
            </button>

            <button
              className="metal-button"
              onClick={async () => {
                await leaveLobby(lobbyId, userID, setError);
                navigate("/lobbyScene");
              }}
            >
              Leave Lobby (Exit)
            </button>
          </div>
        </div>
      </div>
    </Layout>
  );

}
