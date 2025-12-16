
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect, useCallback } from "react";
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
 * @author Kajsa Alice Ulrika Berlstedt
 * @author Lizette Bloch Dahl Nikolajsen
 */
export default function JoinLobby() {
  const navigate = useNavigate();
  const userID: string | null = sessionStorage.getItem("userID");
  //const [lobbyId, setLobbyId] = useState<string>("");
  const [lobbies, setLobbies] = useState<Lobby[]>([]);
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  const [searchTerm, setSearchTerm] = useState<string>("");

  /**
   * @author Niklas Emil Lysdal
   */
  const updateLobbyList = useCallback(async () => {
    setError("");

    try {
      const response = await fetch(API_BASE_URL + "/api/lobby/seeLobbies", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
        },
      });

      const data: lobbyInfo[] = await response.json();
      console.log("received lobby data:", data);
      if (response.ok) {
        const parsedData: Lobby[] = data.map((input: any) => ({
          lobbyInfo: {
            lobbyID: input.lobbyID,
            capacity: input.capacity,
            playerCount: input.playerCount,
            lobbyName: input.lobbyName,
            isRunning: input.isRunning,
            joinable: input.canJoin
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
  }, [setLobbies, setError, API_BASE_URL]);

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
  }, [updateLobbyList]);

  /**
   * @author Bjarke Søderhamn Petersen
   * @author Niklas Emil Lysdal
   * @author Asger Allin Jensen
   * @author Kajsa Alice Ulrika Berlstedt
   */
  const joinLobby = async (id: string): Promise<boolean> => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/lobby/join", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
        },
        body: JSON.stringify({ lobbyID: id }),
      });

      const data: string = await response.text();
      console.log("data received: " + data);

      if (response.status === 201) {
        //setLobbyId(data);
        sessionStorage.setItem("id", data);
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
      <div className="page-title">
        <h1 className="metal-text">Join Lobby</h1>
      </div>
      <div className="lobbies-panel">
        {lobbies.length > 0 ? (
          <div className="lobbies-terminal">
            <h2 className="terminal-title">Available Lobbies</h2>
            <div className="terminal-search">
              <input
                type="text"
                className="terminal-input"
                placeholder="Search by lobby name"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
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
                {lobbies
                  .filter((lobby) => {
                    const term = searchTerm.trim().toLowerCase();
                    if (!term) return true; // no search term, include all
                    const name = lobby.lobbyInfo.lobbyName?.toLowerCase() || "";
                    return name.includes(term);
                  })
                  .map((lobby, index) => {
                    const isFull = lobby.playerCount === lobby.capacity;
                    const canJoin: boolean = lobby.canJoin;
                    const isDisabled = isFull || !canJoin;
                    const rowClassName = isDisabled ? "lobby-item text-red" : "lobby-item";
                    return (
                      <tr key={index} className={rowClassName}>
                        <td className="lobby-table-name">{lobby.lobbyName}</td>
                        <td className="lobby-table-players"> {lobby.playerCount}/{lobby.capacity}</td>
                        <td className="lobby-table-status">{lobby.isRunning ? 'Running' : 'Preparing'}</td>
                        <td>
                          <button
                            className="metal-button icon"
                            onClick={async () => {
                              if (await joinLobby(lobby.lobbyID)) {
                                navigate("/lobbyCreationScene");
                              }
                            }}
                          >
                            Join
                          </button>
                        </td>
                      </tr>
                    )
                  })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <p className="empty-state-text">No lobbies to join</p>
          </div>
        )}

        {error && <p className="error-text">{error}</p>}

        <div className="button-row">
          <button className="metal-button icon" onClick={updateLobbyList}>
            <div className="reload-icon"></div>
          </button>

          <button className="metal-button icon" onClick={() => navigate("/lobbyScene")}>
            <div className="exit-icon"></div>
          </button>
        </div>
      </div>
    </Layout>
  );
}
