/*
Author(s): Bjarke
*/

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";

export default function Lobby() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  
  // commented out to preserve lobbyId (might be bugging the lobby creation/joining flow)
  /*useEffect(() => {
    setLobbyId("");
    setError("");
  }, []);
  */
  
  const createLobby = async () => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL+"/api/lobby/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userID: userID }),
      });

      const data = await response.text();
      console.log("data recived: " + data);
      if (response.status === 201) {
        localStorage.setItem("id", data);
        setLobbyId(data);
      } else {
        setError("An error occurred. Try again.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Try again.");
    }
  };
//lobby menu
  return (
    <Layout>
      <h1>Command Center</h1> 
      <div className="lobby-actions">
  <div className="control-panel">
    <button
      className="metal-button"
      onClick={async () => {
        await createLobby();
        navigate("/lobbyCreationScene");
      }}
    >
      Create Lobby
    </button>

    <button className="metal-button" onClick={() => navigate("/lobbyJoinScene")}>
      Join Lobby
    </button>

    <button className="metal-button" onClick={() => navigate("/")}>
      Go to homepage
    </button>
  </div>
</div>
    </Layout>
  );
}
