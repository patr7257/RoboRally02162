import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect } from "react";
import { useDemoService } from "../services/demoService";
import "./loadLobby.css";
import "../styles/joinLobby.css";


/** 
* @author William Pii Jæger
* @author Karl Johannes Agerbo
*/
export default function LoadLobby() {
  const navigate = useNavigate();
  const userID: string | null = sessionStorage.getItem("userID");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  const { lobbies, lobbyId, getDemos, loadAndStartDemoGame } = useDemoService();

  useEffect(() => {
    getDemos(setError);
    console.log(lobbies);
  }, []);

  return (
    <Layout>
      <div className="page-title">
        <h1 className="metal-text">Demo Games</h1>
      </div>
      <div className="control-panel">
        {lobbies.length > 0 ? (
          <div className="lobbies-terminal">
            <h2 className="terminal-title">Saved Games</h2>
            <ul className="terminal-list">
              {lobbies.map((demoName, index) => (
                <li key={index} className="terminal-item">
                  <span className="terminal-id">{demoName}</span>
                  <button
                    className="metal-button small"
                    onClick={async () => {
                      const newLobbyId = await loadAndStartDemoGame(demoName);
                      if (newLobbyId) {
                        sessionStorage.setItem("id", newLobbyId);
                        sessionStorage.setItem("mode", "demo");
                        navigate("/boardScene");
                      }
                    }}
                  >
                    Start
                  </button>
                </li>
              ))}
            </ul>
          </div>
        ) : (
          <div className="empty-state">
            <p className="empty-state-text">No games to load</p>
          </div>
        )}

        {error && <p className="error-text">{error}</p>}

        <div className="button-row">
          <button className="metal-button icon" onClick={() => navigate("/lobbyScene")}>
            <div className="exit-icon"></div>
          </button>
        </div>
      </div>
    </Layout>
  );
}
