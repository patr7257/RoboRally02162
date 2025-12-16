import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect, useCallback } from "react";
import { subscribe } from "../utils/ws";
import "../styles/joinLobby.css";

interface GameInfo {
  lobbyName: string;
  playerCount: number;
  winner: string | null;
}

/**
 * @author Benjamin Benyo Endahl Hansen
 * @author Lizette Bloch Dahl Nikolajsen
 */

export default function SeeAllGames() {
  const navigate = useNavigate();
  const [savedGames, setAllGames] = useState<GameInfo[]>([]);
  const [error, setError] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  /**
  * @author Benjamin Benyo Endahl Hansen
  */
  const seeAllGames = useCallback(async () => {
    setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/game/seeAllGames", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
        },
      });

      if (!response.ok) {
        setError("An error occurred. Try again.");
        return;
      }

      const data: GameInfo[] = await response.json();
      setAllGames(data);

    } catch (err) {
      console.error("Fetch error:", err);
      setError("Network error. Try again.");
    }
  }, [API_BASE_URL]);


  /**
  * @author Benjamin Benyo Endahl Hansen
  */
  useEffect(() => {
    seeAllGames();

    const unsubscribe = subscribe((message: string) => {
      try {
        const data = JSON.parse(message);
        if (data.type === "games" && data.action === "updatedGames") {
          seeAllGames();
        }
      } catch {
        console.log("Non-JSON WS message:", message);
      }
    });

    return () => unsubscribe();
  }, [seeAllGames]);

  const getWinnerText = (winner: string | null) => {
    return winner ? winner : "In progress";
  };

  return (
    <Layout>
      <div className="page-title">
        <h1 className="metal-text">GAME ADMINSTRATION</h1>
      </div>
      <div className="lobbies-panel">
        {savedGames.length > 0 ? (
          <div className="lobbies-terminal">
            <h2 className="terminal-title">All Games</h2>

            <div className="terminal-search">
              <input
                type="text"
                id="searchGameId"
                name="searchGameId"
                className="terminal-input"
                placeholder="Search by GameID"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            <table className="terminal-table">
              <thead>
                <tr>
                  <th className="terminal-table-header">Lobby Name</th>
                  <th className="terminal-table-header">Players</th>
                  <th className="terminal-table-header">Winner</th>
                </tr>
              </thead>

              <tbody>
                {savedGames
                  .filter(g =>
                    !searchTerm ||
                    g.lobbyName.toLowerCase().includes(searchTerm.toLowerCase())
                  )
                  .map((game, idx) => (
                    <tr key={idx} className="lobby-item">
                      <td className="lobby-table-id">{game.lobbyName}</td>
                      <td className="lobby-table-players">{game.playerCount}</td>
                      <td className="lobby-table-winner">{getWinnerText(game.winner)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <p className="empty-state-text">No games found</p>
          </div>
        )}

        {error && <p className="error-text">{error}</p>}

        <div className="button-row">
          <button className="metal-button icon" onClick={seeAllGames}>
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