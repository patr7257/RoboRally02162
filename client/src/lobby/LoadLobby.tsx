import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect, useCallback } from "react";
import { subscribe } from "../utils/ws";


/** 
* @author Bjarke Søderhamn Petersen
* @author Karl Johannes Agerbo
* @author Benjamin Benyo Endahl Hansen
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
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

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
      console.log("data received: " + data);

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
      console.log("data received: " + data);

      if (response.status === 200) {
        console.log("Game deleted successfully");
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
      <div className="panel-container">
        <h1 className="panel-title">Mission Access Terminal</h1>

        <div className="control-panel">
          <button className="metal-button" onClick={seeSavedGames}>
            See Saved Games
          </button>

          <button className="metal-button" onClick={() => navigate("/lobbyScene")}>
            Return to Command Center
          </button>

          {savedGames.length > 0 && (
            <div className="lobbies-terminal">
              <h2 className="terminal-title">Saved Games</h2>
              <ul className="terminal-list">
                {savedGames.map((game, index) => (
                  <li key={index} className="terminal-item">
                    <span className="terminal-id">{game.lobbyName}</span>
                    <button
                      className="metal-button small"
                      onClick={async () => {
                        if (await loadGame(game.saveID)) {
                          navigate("/lobbyCreationScene");
                        }
                      }}
                    >
                      Continue
                    </button>

                    <button
                      className="metal-button small"
                      onClick={async () => {
                        const deleted = await deleteSavedGame(game.saveID);
                        if (deleted) {
                          await seeSavedGames();
                        }
                      }}
                    >
                      Delete
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
