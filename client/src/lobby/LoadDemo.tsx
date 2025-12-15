import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect } from "react";
import { useDemoService } from "../services/demoService";


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
      <div className="panel-container">
        <h1 className="panel-title">Demo Games</h1>

        <div className="control-panel">

          <button className="metal-button" onClick={() => navigate("/lobbyScene")}>
            Return to Command Center
          </button>

          {lobbies.length > 0 && (
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
          )}

          {error && <p className="error-text">{error}</p>}
        </div>
      </div>
    </Layout>
  );
}
