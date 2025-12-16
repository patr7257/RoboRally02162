import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import { closeSocket } from "../utils/ws";

/**
 * @author Bjarke Søderhamn Petersen
 * @author Karl Johannes Agerbo
 */
export default function Lobby() {
  const navigate = useNavigate();
  const userID: string | null = sessionStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>(sessionStorage.getItem("id") || "");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  /**
 * @author Lizette Bloch Dahl Nikolajsen
 */
  const handleLogout = async () => {
    const userID = sessionStorage.getItem("userID");

    if (userID) {
      try {
        const response = await fetch(
          API_BASE_URL + "/api/users/logout?userID=" + userID,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
            }
          }
        );

        if (!response.ok) {
          console.error("Logout failed with status:", response.status);
        }
      } catch (err) {
        console.error("Logout request error:", err);
      }
    }

    // Always clear frontend session, even if backend fails
    sessionStorage.removeItem("token");
    sessionStorage.removeItem("username");
    sessionStorage.removeItem("userID");

    closeSocket(1000);
    navigate("/");
  };

  return (
    <Layout>
      <div className="page-title">
        <h1 className="metal-text">MAIN MENU</h1>
      </div>
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

          <button
            className="metal-button"
            onClick={async () => {
              navigate("/demoLoadScene");
            }}
          >
            Demo Games
          </button>

          <button className="metal-button" onClick={() => navigate("/seeAllGamesScene")}>
            Game Administration
          </button>

          <button className="metal-button" onClick={() => navigate("/userSettings")}>
            User Settings
          </button>

          <button className="metal-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

    </Layout>
  );
}
