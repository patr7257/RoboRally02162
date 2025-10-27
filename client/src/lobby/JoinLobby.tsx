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
      <div className="lobby-actions">
        <button className="big-button" onClick={findLobby}>
          See lobbies
        </button>
        <button className="big-button" onClick={() => navigate("/lobbyScene")}>
          Go back to lobby menu
        </button>
      </div>

      {lobbies.length > 0 && (
        <div className="lobbies-list">
          <ul>
            {lobbies.map((lobby, index) => (
              <li key={index}>
                <div className="lobbyContainer">
                  Lobby ID: {lobby.lobbyID}
                  <button
                    className="join-button"
                    onClick={async () => {
                      if (await joinLobby(lobby.lobbyID)) {
                      navigate("/lobbyCreationScene");
                      }
                    }}
                  >
                    Join
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

  {error && <p className="error-text">{error}</p>}
    </Layout>
  );
}
