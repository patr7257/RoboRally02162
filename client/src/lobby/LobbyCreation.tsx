import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import { useEffect, useCallback } from "react";
import React, { useState } from "react";
import { subscribe } from "../utils/ws";
import { leaveLobby } from './LeaveLobby';
import { fullLobbyInfo, DEFAULT_FULL_LOBBY_INFO } from '../types/lobbyTypes';

/**
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export default function LobbyCreation() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [error, setError] = useState<string>("");

  const [fullLobbyInfo, setFullLobbyInfo] = useState<fullLobbyInfo>(DEFAULT_FULL_LOBBY_INFO);

  const [isReady, setIsReady] = useState<boolean>(false); //local readiness for button
  const [playersReady, setPlayersReady] = useState<{ [username: string]: boolean }>({}); // total readiness

  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  /**
   * @author Niklas Emil Lysdal
   */
  const updateLobbyInfo = useCallback(async () => {
    try {
      console.log("updating lobby info using id:" + localStorage.getItem("id"));
      const response = await fetch(API_BASE_URL + "/api/lobby/lobbyInfo", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lobbyID: localStorage.getItem("id"), userID: userID }),
      });
      if (!response.ok) {
        throw new Error("Server returned error when  getting lobby info");
      }

      const parsedData = await response.json();
      const lobbyInfo: fullLobbyInfo = parsedData as fullLobbyInfo;


      setFullLobbyInfo(lobbyInfo);
      setPlayersReady(lobbyInfo.readinessMap);
      console.log("lobbyID from parsed:" + fullLobbyInfo?.lobbyID);
    } catch (err) {
      console.error("updateLobbyInfo error lobby error:", err);
      console.log("lobbyID from parsed:" + fullLobbyInfo?.lobbyID);
      if (setError) setError("Network error. Try Again.");
    }
  }, [userID, setFullLobbyInfo, setPlayersReady, setError, API_BASE_URL]);

/**
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
  useEffect(() => {
    updateLobbyInfo();
    const unsubscribe = subscribe((message: string) => {
      console.log("Received message:", message);

      try {
        const data = JSON.parse(message);

        if (data.type === "lobby" && data.action === "lobbyUpdate") {
          console.log("updating lobby info based on notification");
          updateLobbyInfo();
          return;
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
      unsubscribe();
    }
  }, [navigate,updateLobbyInfo])

/**
 * @author Asger Allin Jensen
 */

  const handleToggleReadiness = async () => {
    setError("");

    const endpoint = isReady ? "markNotReady" : "markReady";

    try {
      const response = await fetch(`${API_BASE_URL}/api/lobby/${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userID: userID,
          lobbyID: fullLobbyInfo?.lobbyID,
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

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
  const startGame = async () => {
    setError("");
    if (fullLobbyInfo === null || fullLobbyInfo === undefined) {
      setError("Something went wrong.")
      return;
    }
    try {
      const response = await fetch(API_BASE_URL + '/api/lobby/start', {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lobbyID: localStorage.getItem("id") }),
      });
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Try again.");
    }
  };

  return (
    <Layout>
      <div className="panel-container">
        <h1 className="panel-title">Mission Setup</h1>
        <div className="lobby-panels-row">
          <div className="readiness-panel">
            <h2>Players: {fullLobbyInfo?.playerCount}/{fullLobbyInfo?.capacity}</h2> {/*unsure if this can be misinterpreted as players ready.*/}

             <div className = "players-list-scroller">

            <ul>
              {Object.entries(playersReady).map(([name, ready]) => (
                <li key={name} className={`player-slot ${ready ? "ready" : "not-ready"}`}>
                  {name}: {ready ? "Ready" : "Not Ready"}
                </li>
              ))}
              {[...Array(fullLobbyInfo.capacity - Object.keys(playersReady).length)].map((_, index) => (
              <li key={`empty-${index}`} className="player-slot empty-slot">
               ---------
              </li>

              ))}

            </ul>
            </div>
          </div>



          <div className="control-panel">
            <div className="lobby-id-display">
              <span className="lobby-name-label">LOBBY</span>
              <span className="lobby-name-value">{fullLobbyInfo.lobbyName ? fullLobbyInfo.lobbyName : (fullLobbyInfo.lobbyID ? fullLobbyInfo!.lobbyID :  "Error")}</span>
            </div>

            <button className="metal-button" onClick={() => startGame()}>
              Start Game
            </button>
            <button className="metal-button" onClick={handleToggleReadiness}>
              {isReady ? "Not Ready" : "Ready"}
            </button>

            <button
              className="metal-button"
              onClick={async () => {
                await leaveLobby(fullLobbyInfo?.lobbyID, userID, setError);
                navigate("/lobbyJoinScene");
              }}
            >
              Back to Lobbies
            </button>

            <button
              className="metal-button"
              onClick={async () => {
                await leaveLobby(fullLobbyInfo?.lobbyID, userID, setError);
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