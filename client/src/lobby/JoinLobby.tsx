/*
Author(s): Bjarke, Niklas, Asger
*/
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState } from "react";

interface Lobby {
  lobbyID: string;
  [key: string]: any;
}

export default function JoinLobby() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>("");
  const [lobbies, setLobbies] = useState<Lobby[]>([]);
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;


  const findLobby = async () => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL+"/api/lobby/seeLobbies", {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      });

      const data = await response.text();
      if (response.ok) {
        localStorage.setItem("lobbies", data);
        setLobbies(JSON.parse(data));
      } else {
        setError("An error occurred. Try again.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Try again.");
    }
  };

  const joinLobby = async (id: string):Promise<boolean>=> {
    setError("");
    try {
      const response = await fetch(API_BASE_URL+"/api/lobby/join", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userID: userID, lobbyID: id }),
      });

      const data = await response.text();
      console.log("data received: " + data);
      
      if (response.status === 201) {
        localStorage.setItem("id", data);
        setLobbyId(id);
        return true;
      } else  if (response.status === 403) { //FORBIDDEN
          setError("Lobby is locked, unable to join.");
          return false;
      }
      else{
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

      <div className="control-panel">
        <button className="metal-button" onClick={findLobby}>
          Scan for Active Lobbies
        </button>

        <button className="metal-button" onClick={() => navigate("/lobbyScene")}>
          Return to Command Center
        </button>

        {lobbies.length > 0 && (
          <div className="lobbies-terminal">
            <h2 className="terminal-title">Available Lobbies</h2>
            <ul className="terminal-list">
              {lobbies.map((lobby, index) => (
                <li key={index} className="terminal-item">
                  <span className="terminal-id">ID: {lobby.lobbyID}</span>
                  <button
                    className="metal-button small"
                    onClick={async () => {
                      if (await joinLobby(lobby.lobbyID)) {
                        navigate("/lobbyCreationScene");
                      }
                    }}
                  >
                    Join
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {error && <p className="error-text">{error}</p>}
      </div>
    </div>
  </Layout>
);
}
