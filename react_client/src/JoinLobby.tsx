/*
Author(s): Bjarke, Niklas
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
  const usernameInput: string | null = localStorage.getItem("username");
  const [lobbyId, setLobbyId] = useState<string>("");
  const [lobbies, setLobbies] = useState<Lobby[]>([]);
  const [error, setError] = useState<string>("");

  const findLobby = async () => {
    setError("");
    try {
      const response = await fetch("http://localhost:8080/api/lobby/seeLobbies", {
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
      const response = await fetch("http://localhost:8080/api/lobby/join", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: usernameInput, lobbyID: id }),
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
      <div
        style={{
          marginTop: "50px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: "12px",
        }}
      >
        <button className="big-button" onClick={findLobby}>
          See lobbies
        </button>
        <button className="big-button" onClick={() => navigate("/lobbyScene")}>
          Go back to lobby menu
        </button>
      </div>

      {lobbies.length > 0 && (
        <div
          style={{
            maxHeight: "200px",
            overflowY: "auto",
            marginTop: "20px",
            padding: "0px",
            border: "1px solid #ffffffff",
            borderRadius: "8px",
            width: "350px",
          }}
        >
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {lobbies.map((lobby, index) => (
              <li
                key={index}
                style={{
                  padding: "8px",
                  marginBottom: "0px",
                  background: "#282c34",
                  borderRadius: "6px",
                  textAlign: "center",
                }}
              >
                <div className="lobbyContainer">
                  Lobby ID: {lobby.lobbyID}
                  <button
                    style={{ padding: 10 }}
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

      {error && <p style={{ color: "red" }}>{error}</p>}
    </Layout>
  );
}
