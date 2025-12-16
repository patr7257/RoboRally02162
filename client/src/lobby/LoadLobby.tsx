import "./loadLobby.css";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect, useCallback } from "react";
import { subscribe } from "../utils/ws";
import "../styles/joinLobby.css";


/** 
* @author Bjarke Søderhamn Petersen
* @author Karl Johannes Agerbo
* @author Benjamin Benyo Endahl Hansen
* @author Lizette Bloch Dahl Nikolajsen
*/
interface Lobby {
  lobbyID: string;
  [key: string]: any;
}

/** 
* @author Bjarke Søderhamn Petersen
* @author Karl Johannes Agerbo
* @author Benjamin Benyo Endahl Hansen
*/
export default function LoadLobby() {
  const navigate = useNavigate();
  const userID: string | null = sessionStorage.getItem("userID");
  const [lobbyId, setLobbyId] = useState<string>("");
  const [savedGames, setSavedGames] = useState<Lobby[]>([]);
  const [error, setError] = useState<string>("");
  const [confirmAction, setConfirmAction] = useState<{
    type: 'load' | 'delete';
    saveID: string;
    lobbyName: string;
  } | null>(null);
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  const [showDeletePopup, setShowDeletePopup] = useState(false);
  const [gameToDelete, setGameToDelete] = useState<string | null>(null);


  /** 
  * @author Bjarke Søderhamn Petersen
  * @author Karl Johannes Agerbo
  * @author Benjamin Benyo Endahl Hansen
  */
  const seeSavedGames = useCallback(async () => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/game/seeSavedGames", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
        },
      });

      const data = await response.text();
      if (response.ok) {
        setSavedGames(JSON.parse(data));
      } else {
        setError("An error occurred. Try again.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Try again.");
    }
  }, [API_BASE_URL]);


  /**
   * @author Benjamin Benyo Endahl Hansen
   */
  useEffect(() => {
    seeSavedGames();

    const unsubscribe = subscribe((message: string) => {
      try {
        const data = JSON.parse(message);
        if (data.type === "games" && data.action === "updatedGames") {
          seeSavedGames();
        }
      } catch {
        console.log("Non-JSON WS message:", message);
      }
    });

    return () => unsubscribe();
  }, [seeSavedGames]);

  const getWinnerText = (winner: string | null) => {
    return winner ? winner : "In progress";
  };

  /** 
  * @author Bjarke Søderhamn Petersen
  * @author Karl Johannes Agerbo
  * @author Benjamin Benyo Endahl Hansen
  */
  const loadGame = async (saveId: string): Promise<boolean> => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/game/loadGame", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
        },
        body: JSON.stringify({ saveID: saveId }),
      });

      const data = await response.text();

      if (response.status === 201) {
        sessionStorage.setItem("id", data);
        setLobbyId(data);
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


  /**
  * @author Benjamin Benyo Endahl Hansen
  */
  const deleteSavedGame = async (saveID: string): Promise<boolean> => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/game/deleteSavedGame", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
        },
        body: JSON.stringify({ saveID }),
      });

      const data = await response.text();

      if (response.status === 200) {
        return true;
      } else if (response.status === 403) {
        setError("Error in deleting game");
        return false;
      } else {
        setError("An error occurred. Try again.");
        return false;
      }
    } catch (err) {
      console.error("Delete error:", err);
      setError("Network error. Try again.");
      return false;
    }
  };

  return (
    <Layout>
      {confirmAction && (
        <div className="confirmation-overlay">
          <div className="confirmation-modal">
            <div className="confirmation-content">
              <p className="confirmation-text">
                {confirmAction.type === 'load'
                  ? `DO YOU WANT TO RESUME THE GAME "${confirmAction.lobbyName.toUpperCase()}"?`
                  : `ARE YOU SURE YOU WANT TO DELETE THE GAME "${confirmAction.lobbyName.toUpperCase()}"?`
                }
              </p>
            </div>
            <div className="confirmation-buttons">
              <button
                className="metal-button icon"
                onClick={async () => {
                  if (confirmAction.type === 'load') {
                    if (await loadGame(confirmAction.saveID)) {
                      navigate("/lobbyCreationScene");
                    }
                  } else {
                    const deleted = await deleteSavedGame(confirmAction.saveID);
                    if (deleted) {
                      await seeSavedGames();
                    }
                  }
                  setConfirmAction(null);
                }}
                aria-label="Yes, confirm action"
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>YES</span>
              </button>
              <button
                className="metal-button icon"
                onClick={() => setConfirmAction(null)}
                aria-label="No, cancel action"
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>NO</span>
              </button>
            </div>
          </div>
        </div>
      )}
      <div className="page-title">
        <h1 className="metal-text">Load Game</h1>
      </div>
      <div className="control-panel">
        {savedGames.length > 0 ? (
          <div className="lobbies-terminal">
            <h2 className="terminal-title">Saved Games</h2>
            <ul className="terminal-list">
              {savedGames.map((game, index) => (
                <li key={index} className="terminal-item">
                  <span className="terminal-id">{game.lobbyName}</span>
                  <button
                    className="metal-button icon"
                    onClick={() => {
                      setConfirmAction({
                        type: 'load',
                        saveID: game.saveID,
                        lobbyName: game.lobbyName
                      });
                    }}
                  >
                    <div className="continue-icon"></div>
                  </button>

                  <button
                    className="metal-button icon"
                    onClick={() => {
                      setConfirmAction({
                        type: 'delete',
                        saveID: game.saveID,
                        lobbyName: game.lobbyName
                      });
                    }}
                  >
                    <div className="delete-icon"></div>
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
          <button className="metal-button icon" onClick={seeSavedGames}>
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