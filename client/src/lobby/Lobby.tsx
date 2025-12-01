import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";

/**
 * @author Bjarke Søderhamn Petersen
 */
export default function Lobby() {
  const navigate = useNavigate();
  const userID: string | null = localStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
   
  return (
    <Layout>
      <h1>Command Center</h1>
      <div className="lobby-actions">
  <div className="control-panel">
    <button
      className="metal-button"
      onClick={async () => {
        navigate("/lobbyCreatorScene");
      }}
    >
      Create Lobby
    </button>

    <button className="metal-button" onClick={() => navigate("/lobbyJoinScene")}>
      Join Lobby
    </button>

    <button className="metal-button" onClick={() => navigate("/lobbyLoadScene")}>
       Continue Game
    </button>

    <button className="metal-button" onClick={() => navigate("/seeAllGamesScene")}>
       Game Administration
    </button>


    <button className="metal-button" onClick={() => navigate("/")}>
      Go to homepage
    </button>
  </div>
</div>

    </Layout>
  );
}
