

import React, { useState, useEffect } from "react";
import { Menu, X } from "lucide-react"; // lightweight icons
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage } from "../utils/ws";
import { MoveType, GameData, HandData, ROBOT_COLORS } from "../types/boardTypes";
import { WinnerBanner } from "./WinnerBanner";
import { BoardRenderer } from "./BoardRenderer";
import { GameControls } from "./GameControls";
import CheckpointChecklist from "../ui/checkpointChecklist";
import { leaveLobby } from '../lobby/LeaveLobby';
import { saveGame } from '../lobby/SaveGame';


/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Patrick Røbel
* @author William Pii Jæger
* @author Kajsa Alice Ulrika Berlstedt
*/

interface ReadinessData {
  playerSubmitted: Record<number, boolean>;
  msRemaining: number;
}

/**
* @author Asger Allin Jensen
* @author Bjarke Søderhamn Petersen
* @author Patrick Røbel
* @author William Pii Jæger
*/
export default function Board() {
  const navigate = useNavigate();
  const [userID] = useState<string>(localStorage.getItem("userID") || "");
  const [lobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [gameData, setGameData] = useState<GameData | null>(null);
  const [handData, setHandData] = useState<HandData | null>(null);
  const [selectedMoves, setSelectedMoves] = useState<(MoveType | null)[]>(Array(5).fill(null));
  const [readiness, setReadiness] = useState<ReadinessData | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<number>(0);
  const [gameState, setGameState] = useState<'waiting' | 'programming' | 'executing' | 'finished'>('waiting');
  const [hasSubmitted, setHasSubmitted] = useState<boolean>(false);
  const [robotID, setRobotID] = useState<string>("");
  const [menuOpen, setMenuOpen] = useState(false);
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  const [robotMap, setRobotMap] = useState<{ [username: string]: string }>({});


  useEffect(() => {
    const unsubscribe = subscribe((message: string) => {
      try {
        const data = JSON.parse(message);

        console.log("Received message:", data);

        let actualData = data;
        if (data.type === "game" && data.payload) {
          actualData = data.payload;
          console.log("Unwrapped game payload:", actualData);
        }

        switch (actualData.type) {
          case "stateSnapshot":
            console.log("Setting game data:", actualData.payload || actualData);
            setGameData(actualData.payload || actualData);
            break;

          case "hand":
            console.log("Setting hand data:", actualData.payload || actualData);

            const handPayload = actualData.payload;
            setHandData(handPayload);
            setSelectedMoves(Array(5).fill(null));
            setHasSubmitted(false);
            break;

          case "readiness":
            setReadiness(data.payload);
            setTimeRemaining(data.payload.msRemaining);
            break;

          case "timeRemaining":
            setTimeRemaining(data.payload.ms);
            break;

          case "programmingStarted":
            setGameState('programming');
            setHasSubmitted(false);

            sendMessage({ lobbyID: lobbyId, payload: { type: "getHand" } });

            startReadinessPolling();
            break;

          case "playerSubmitted":
            sendMessage({ lobbyID: lobbyId, payload: { type: "getReadiness" } });
            break;

          case "roundExecuting":
            setGameState('executing');
            stopReadinessPolling();
            break;

          case "update":
            sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
            break;

          case "gameFinished":
            setGameState('finished');
            stopReadinessPolling();
            sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
            break;

          case "gameSaved":
            navigate("/");

          case "ack":
            console.log("Command acknowledged:", data.payload.message);
            if (data.payload.message === "Program submitted") {
              setHasSubmitted(true);
            }
            break;

          case "error":
            console.error("Server error:", data.payload);
            alert(`Error: ${data.payload.message}`);
            break;

          default:
            console.log("Unknown message type:", data.type, data);
        }
      } catch (e) {
        console.log("Raw text message:", message);
      }
    });

    sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
    sendMessage({ lobbyID: lobbyId, payload: { type: "getHand" } });
    getRobotIDS();

    return () => {
      stopReadinessPolling();
      if (unsubscribe) unsubscribe();
    };
  }, [lobbyId]);

  let readinessInterval: NodeJS.Timeout | null = null;

  const startReadinessPolling = () => {
    stopReadinessPolling();
    readinessInterval = setInterval(() => {
      sendMessage({ lobbyID: lobbyId, payload: { type: "getReadiness" } });
    }, 1000);
  };

  const stopReadinessPolling = () => {
    if (readinessInterval) {
      clearInterval(readinessInterval);
      readinessInterval = null;
    }
  };


  const getRobotIDS = async () => {
    console.log("Fetching robot ID for user:", userID);

    try {
      const response = await fetch(`${API_BASE_URL}/api/lobby/getRobot`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          lobbyID: lobbyId,
          userID: userID
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error("Failed to fetch robot IDs:", errorText);
        return;
      }

      const data = await response.json();
      console.log("Robot ID map received:", data);
      setRobotMap(data);

      const username = localStorage.getItem("username");
      if (!username) {
        console.warn("No username found in localStorage");
        return;
      }

      const id = data[username];

      if (id) {
        console.log(`Your robot ID is: ${id}`);
        setRobotID(id.toString());
        localStorage.setItem("robotID", id.toString()); // optional persistence
      } else {
        console.warn(`No robot ID found for username "${username}" in`, data);
      }

    } catch (error) {
      console.error("Error fetching robot ID:", error);
    }
  };



  const handleSubmitMove = (moves: MoveType[]) => {
    if (moves.length === 0) {
      alert("Please select at least one move");
      return;
    }
    if (hasSubmitted) {
      alert("You have already submitted for this round");
      return;
    }
    if (gameState !== 'programming') {
      alert("Cannot submit moves outside of programming phase");
      return;
    }

    sendMessage({
      lobbyID: lobbyId,
      payload: {
        type: "submitProgram",
        cards: moves
      }
    });
  };

  const handleStartProgramming = () => {
    sendMessage({
      lobbyID: lobbyId,
      payload: {
        type: "startProgramming",
        windowMs: 60000
      }
    });
  };

  const formatTimeRemaining = (ms: number): string => {
    const seconds = Math.ceil(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const getReadinessDisplay = (): string => {
    if (!readiness) return '';
    const submitted = Object.values(readiness.playerSubmitted).filter(Boolean).length;
    const total = Object.keys(readiness.playerSubmitted).length;
    return `${submitted}/${total} players ready`;
  };

  const renderRobotLabels = (): React.ReactNode => {
    const username = localStorage.getItem("username") || "";
    const entries = Object.entries(robotMap);
    if (!robotID || entries.length === 0) {
      return
        <div className="player-info">
          No players are assigned yet
        </div>;
    }
    const list = entries
        .map(([name, idStr]) => {
            const idNum = Number(idStr);
            return { name, idStr: String(idStr), idNum: Number.isNaN(idNum) ? Infinity : idNum };
            })
            .sort((a, b) => a.idNum - b.idNum || a.name.localeCompare(b.name));
    const meIndex = list.findIndex(item => item.name === username);
      if (meIndex > -1) {
        const [me] = list.splice(meIndex, 1);
        list.unshift(me);
      }

    return (
        <div className="player-list">
          {list.map(({ name, idStr, idNum }) => {
            const colorIndex = !Number.isNaN(idNum) && idNum > 0 ? (idNum - 1) % ROBOT_COLORS.length : 0;
            const color = ROBOT_COLORS[colorIndex] ?? "#000";
            const isMe = name === username;

    return (
      <div key={name} className="player-info">
        {isMe ? (
        <>You are: <span className="robot-label" style={{ color }}>{`Robot ${robotID}`}</span></>
                    ) : (
                      <>{name}: <span className="robot-label" style={{ color }}>{`Robot ${robotID}`}</span></>
                    )}
                  </div>
                );
              })}
      </div>
    );
  };

  return (
    <div className="board-Master">
      {gameData?.game?.winner != null && (
        <WinnerBanner winnerId={gameData.game.winner} />
      )}

      <div className="board-Left">
        <div className="boardContainer">
          <BoardRenderer gameData={gameData} />
        </div>

      </div>


      <div className="board-Right">
        <div className="info">
          <div className="navigation relative">
            <button
              className="burger-menu-btn"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {menuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>

            <div className={`menu-dropdown ${menuOpen ? 'open' : ''}`}>
              <button
                className="go-home-btn"
                onClick={() => {
                  leaveLobby(lobbyId, localStorage.getItem("userID"), null);
                  navigate("/");
                }}
              >
                Go to Homepage
              </button>

              <button
                className="go-home-btn"
                onClick={() => saveGame(lobbyId)}
                disabled={gameState === "executing"}
              >
                Save and Quit
              </button>
            </div>
          </div>
          {renderRobotLabels()}
          <div className="game-state-info">
            <div className="state-badge">
              State: <strong>{gameState.toUpperCase()}</strong>
            </div>

            {gameState === 'programming' && (
              <>
                <div className="timer">
                  Time Remaining: <strong>{formatTimeRemaining(timeRemaining)}</strong>
                </div>
                <div className="readiness">
                  {getReadinessDisplay()}
                </div>
                {hasSubmitted && (
                  <div className="submitted-indicator">
                    ✓ You have submitted your program
                  </div>
                )}
              </>
            )}

            {gameState === 'executing' && (
              <div className="executing-message">
                Round is executing...
              </div>
            )}

            {gameState === 'waiting' && (
              <button
                className="start-programming-btn"
                onClick={handleStartProgramming}
              >
                Start Programming Phase
              </button>
            )}
            {gameData && (
              <CheckpointChecklist board={gameData.board} robots={gameData.robots} />
            )}
            {gameState === 'programming' && readiness && (
              <div className="readiness-details">
                <h3>Player Status</h3>
                <ul>
                  {Object.entries(readiness.playerSubmitted).map(([playerId, submitted]) => (
                    <li key={playerId}>
                      Player {playerId}: {submitted ? '✓ Submitted' : ' Waiting'}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
        <div className="controls">
          <GameControls
            selectedMoves={selectedMoves}
            onSubmitMove={handleSubmitMove}
            onSelectMove={setSelectedMoves}
            hand={handData?.hand || []} discard={[]}          />
        </div>
      </div>
    </div>

  );
}