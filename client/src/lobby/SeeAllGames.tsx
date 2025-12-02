import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState, useEffect, useCallback } from "react";
import { subscribe } from "../utils/ws";

interface GameInfo {
  lobbyName: string;
  playerCount: number;
  winner: string | null;
}

/**
 * @author Benjamin Benyo Endahl Hansen
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
      <div className="panel-container">
        <h1 className="panel-title">Mission Access Terminal</h1>

        <div className="lobbies-panel">
          <button className="metal-button" onClick={seeAllGames}>
            See All Games
          </button>

          <button className="metal-button" onClick={() => navigate("/lobbyScene")}>
            Return to Command Center
          </button>

          {savedGames.length > 0 && (
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
          )}

          {error && <p className="error-text">{error}</p>}
        </div>
      </div>
    </Layout>
  );
}