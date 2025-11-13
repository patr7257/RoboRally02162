
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect } from "react";
import { lobbyInfo } from "../types/lobbyTypes";
import "../styles/joinLobby.css";
import { subscribe } from "../utils/ws";


interface Lobby {
  lobbyInfo: lobbyInfo;
  [key: string]: any;
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 * @author Asger Allin Jensen
 */
export default function JoinLobby() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>("");
  const [lobbies, setLobbies] = useState<Lobby[]>([]);
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  /**
   * @author Niklas Emil Lysdal
   */
  const updateLobbyList = async () => {
    setError("");

    try {
      const response = await fetch(API_BASE_URL + "/api/lobby/seeLobbies", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userID: localStorage.getItem("userID") }),
      });

      const data:lobbyInfo[] = await response.json();
      console.log("received lobby data:", data);
      if (response.ok) {
        const parsedData: Lobby[] = data.map((input: any) => ({
          lobbyInfo: {
            lobbyID: input.lobbyID,
            capacity: input.capacity,
            playerCount: input.playerCount,
            lobbyName: input.lobbyName,
            status: input.isRunning
          },
          ...input
        }))


        setLobbies(parsedData);
      } else {
        setError("An error occurred. Try again.");
      }
    } catch (err) {
      console.error("SeeLobbies error:", err);

      setError("Network error. Try again.");
    }
  };

    /**
   * @author Niklas Emil Lysdal
   */
  useEffect(() => {
    updateLobbyList();
    const unsubscribe = subscribe((message: string) => {
      try {
        const data = JSON.parse(message);
        console.log("Parsed data:", data);
        if (data.type === "lobbies" && data.action === "updatedLobbies") {
          updateLobbyList();
          return;
        }
      } catch {
        console.log("Raw text message:", message);
      }
    });
    return () => {
      unsubscribe();
    }
  },[]);

/**
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 * @author Asger Allin Jensen
 */
  const joinLobby = async (id: string): Promise<boolean> => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/lobby/join", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userID: userID, lobbyID: id }),
      });

      const data:string = await response.text();
      console.log("data received: " + data);

      if (response.status === 201) {
        setLobbyId(data);
        localStorage.setItem("id",data);
        return true;
      } else if (response.status === 403) { //FORBIDDEN
        setError("Lobby is locked, unable to join.");
        return false;
      }
      else {
        setError("An error occurred. Try again.");
        return false;
      }
    } catch (err) {
      console.error("Join error:", err);
      setError("Network error. Try again.");
      return false;
    }
  };





  return (
    <Layout>
      <div className="panel-container">
        <h1 className="panel-title">Mission Access Terminal</h1>

        <div className="lobbies-panel">
          <button className="metal-button" onClick={updateLobbyList}>
            Scan for Active Lobbies
          </button>

          <button className="metal-button" onClick={() => navigate("/lobbyScene")}>
            Return to Command Center
          </button>

          {lobbies.length > 0 && (
            <div className="lobbies-terminal">
              <h2 className="terminal-title">Available Lobbies</h2>
              <table className="terminal-table">

                <thead>
                  <tr>
                    <th className="terminal-table-header">Name</th>
                    <th className="terminal-table-header">Players</th>
                    <th className="terminal-table-header"> Status</th>
                    <th className="terminal-table-button-header"></th> {/* for the join button*/}

                  </tr>
                </thead>

                <tbody>
                  {lobbies.map((lobby, index) => {
                    const isFull = lobby.lobbyInfo.playerCount===lobby.lobbyInfo.capacity;
                    const isRunning = lobby.lobbyInfo.status;
                    const isDisabled = isFull || isRunning; 
                    const rowClassName = isDisabled ? "lobby-item text-red" : "lobby-item";
                    return (
                    <tr key={index} className={rowClassName}>
                      <td className="lobby-table-name">{lobby.lobbyInfo.lobbyName}</td>
                      <td className="lobby-table-players"> {lobby.lobbyInfo.playerCount}/{lobby.lobbyInfo.capacity}</td>
                      <td className="lobby-table-status">{lobby.lobbyInfo.status ? 'Running' : 'Preparing'}</td>
                      <td>
                        <button
                          className="metal-button small"
                          onClick={async () => {
                            if (await joinLobby(lobby.lobbyInfo.lobbyID)) {
                              navigate("/lobbyCreationScene");
                            }
                          }}
                        >
                          Join
                        </button>
                      </td>
                    </tr>
                  )})}
                </tbody>
              </table>
            </div>
          )}

          {error && <p className="error-text">{error}</p>}
        </div>
      </div>
    </Layout>
  );
}
