import React, { useState, useEffect, useRef } from "react";
import { Menu, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage } from "../utils/ws";
import { MoveType, GameData, HandData, ROBOT_COLORS, DiscardData } from "../types/boardTypes";
import { WinnerBanner } from "./WinnerBanner";
import { BoardRenderer } from "./BoardRenderer";
import { GameControls } from "./GameControls";
import ReactionPopUp from "./actionSelector";
import CheckpointChecklist from "../ui/checkpointChecklist";
import { leaveLobby } from "../lobby/LeaveLobby";
import { RespawnDirectionModal } from "../ui/RespawnDirectionModal";

interface ReadinessData {
  playerSubmitted: Record<number, boolean>;
  msRemaining: number;
}

/**
 * @author Asger Allin Jensen
 * @author Bjarke Søderhamn Petersen
 * @author Patrick Røbel
 * @author William Pii Jæger
 * @author Kajsa Alice Ulrika Berlstedt
 * @author Benjamin Benyo Endhal Hansen
 * @author Karl Johannes Agerbo
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Weihao Mo
 */
export default function Board() {
  const navigate = useNavigate();
  const [userID] = useState<string>(localStorage.getItem("userID") || "");
  const [lobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [gameData, setGameData] = useState<GameData | null>(null);
  const [handData, setHandData] = useState<HandData | null>(null);
  const [discardData, setDiscardData] = useState<DiscardData | null>(null);
  const [selectedMoves, setSelectedMoves] = useState<(MoveType | null)[]>(
    Array(5).fill(null)
  );
  const [readiness, setReadiness] = useState<ReadinessData | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<number>(0);
  const [gameState, setGameState] = useState<
    "waiting" | "programming" | "executing" | "finished" | "waitingForRespawn" | "reaction"
  >("waiting");
  const [hasSubmitted, setHasSubmitted] = useState<boolean>(false);
  const [robotID, setRobotID] = useState<string>("");
  const [menuOpen, setMenuOpen] = useState(false);
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  const [robotMap, setRobotMap] = useState<{ [username: string]: string }>({});
  const [needsRespawn, setNeedsRespawn] = useState<boolean>(false);
  const [respawnRobotId, setRespawnRobotId] = useState<number | null>(null);
  const [mapDisplayName, setMapDisplayName] = useState<string>("");
  const [startingAreaInfo, setStartingAreaInfo] = useState<{
    direction: string;
    width: number;
    height: number;
  } | null>(null);

  const [reactionPopup, setReactionPopup] = useState<{
    kind: string;
    options: string[];
    deadline?: number;
  } | null>(null);

  const readinessIntervalRef = useRef<NodeJS.Timeout | null>(null);


  // Fetch lobby info and full board template for Map Banner and starting area info
  useEffect(() => {
    const fetchLobbyInfo = async () => {
      try {
        const response = await fetch(API_BASE_URL + "/api/lobby/lobbyInfo", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ userID, lobbyID: lobbyId }),
        });

        if (response.ok) {
          const lobbyInfo = await response.json();
          const templateName = lobbyInfo.boardTemplateName || "";

          // Fetch template info to get display name
          const templatesResponse = await fetch(
            API_BASE_URL + "/api/templates/list",
            {
              method: "GET",
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${localStorage.getItem("userToken")}`,
              },
            }
          );

          if (templatesResponse.ok) {
            const templates = await templatesResponse.json();
            const template = templates.find((t: any) => t.name === templateName);
            setMapDisplayName(template?.displayName || templateName);
          } else {
            setMapDisplayName(templateName);
          }

          // Get all template infos
          if (templateName && templateName !== "Random") {
            const templateResponse = await fetch(API_BASE_URL + "/api/templates/get", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${localStorage.getItem("userToken")}`
              },
              body: JSON.stringify({ templateName }),
            });
            if (templateResponse.ok) {
              const fullTemplate = await templateResponse.json();
              if (fullTemplate.startingBoardDirection) {
                setStartingAreaInfo({
                  direction: fullTemplate.startingBoardDirection.toUpperCase(),
                  width: fullTemplate.startingBoardWidth || 0,
                  height: fullTemplate.startingBoardHeight || 0,
                });
              }
            }
          }
        }
      } catch (error) {
        console.error("Failed to fetch lobby info:", error);
      }
    };

    if (userID && lobbyId) {
      fetchLobbyInfo();
    }
  }, [userID, lobbyId, API_BASE_URL]);

  /**
  * @author Asger Allin Jensen
  * @author Bjarke Søderhamn Petersen
  * @author William Pii Jæger
  * @author Kajsa Alice Ulrika Berlstedt
  * @author Benjamin Benyo Endhal Hansen
  * @author Karl Johannes Agerbo
  * @author Lizette Bloch Dahl Nikolajsen
  * @author Weihao Mo
  */
  useEffect(() => {
    const unsubscribe = subscribe((message: string) => {
      try {
        const data = JSON.parse(message);

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

          case "discard":
            console.log("Setting discard data:", actualData.payload || actualData);
            const discardPayload = actualData.payload;
            setDiscardData(discardPayload);
            break;

          case "timeRemaining":
            setTimeRemaining(data.payload.ms);
            break;

          case "programmingStarted":
            setGameState('programming');
            setHasSubmitted(false);
            sendMessage({ lobbyID: lobbyId, payload: { type: "getDiscard" } });
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
            window.dispatchEvent(new Event('roundExecuted'));
            break;

          case "gameFinished":
            setGameState('finished');
            stopReadinessPolling();
            sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
            break;

          case "reactionNeeded":
            setGameState("reaction");
            console.log(actualData)
            if (actualData.payload?.kind && actualData.payload?.options) {
              setReactionPopup({
                kind: actualData.payload.kind,
                options: actualData.payload.options,
                deadline: actualData.payload.deadline
              });
            }
            break;

          case "gameSaved":
            console.log("Game has been saved!");
            break;

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

          case "needRespawnDirection":
            console.log("Robot needs respawn direction:", actualData.payload);
            const deadRobotId = actualData.payload?.robotId;
            const currentRobotId = parseInt(robotID);
            console.log(`Dead robot ID: ${deadRobotId}, Current robot ID: ${currentRobotId}, Raw robotID: ${robotID}`);

            if (deadRobotId === currentRobotId) {
              console.log("This player's robot died - showing respawn modal");
              setNeedsRespawn(true);
              setRespawnRobotId(deadRobotId);
            } else {
              console.log("Different player's robot died - not showing modal");
            }
            break;

          default:
            console.log("Unknown message type:", data.type, data);
        }
      } catch (e) {
        console.log("Raw text message:", message);
      }
    });

    sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
    sendMessage({ lobbyID: lobbyId, payload: { type: "getDiscard" } });
    sendMessage({ lobbyID: lobbyId, payload: { type: "getHand" } });

    getRobotIDS();

    return () => {
      stopReadinessPolling();
      if (unsubscribe) unsubscribe();
    };
  }, [lobbyId, robotID]);

  /**
   * @author Asger Allin Jensen
   */
  useEffect(() => {
    if (!reactionPopup?.deadline) return;

    const now = Date.now();
    const remaining = reactionPopup.deadline - now;

    if (remaining <= 0) {
      setReactionPopup(null);
      return;
    }

    const timer = setTimeout(() => {
      setReactionPopup(null);
    }, remaining);

    return () => clearTimeout(timer);
  }, [reactionPopup]);

  /**
   * @author William Pii Jæger
   */
  const startReadinessPolling = () => {
    stopReadinessPolling();
    readinessIntervalRef.current = setInterval(() => {
      sendMessage({ lobbyID: lobbyId, payload: { type: "getReadiness" } });
    }, 1000);
  };

  /**
   * @author William Pii Jæger
   */
  const stopReadinessPolling = () => {
    if (readinessIntervalRef.current) {
      clearInterval(readinessIntervalRef.current);
      readinessIntervalRef.current = null;
    }
  };

  /**
   * @author Weihao Mo
   */
  const handleRespawnDirection = (direction: 'NORTH' | 'SOUTH' | 'EAST' | 'WEST') => {
    if (!respawnRobotId) {
      console.error("No respawn robot ID set");
      return;
    }

    console.log(`Sending respawn direction ${direction} for robot ${respawnRobotId}`);

    const backendDirection = direction.charAt(0);

    sendMessage({
      lobbyID: lobbyId,
      playerID: parseInt(robotID),
      payload: {
        type: "setRespawnDirection",
        direction: backendDirection
      }
    });

    setNeedsRespawn(false);
    setRespawnRobotId(null);
  };

  /**
   * @author Kajsa Alice Ulrika Berlstedt
   * @author Asger Allin Jensen
   */
  const getRobotIDS = async () => {
    console.log("Fetching robot ID for user:", userID);

    try {
      const response = await fetch(`${API_BASE_URL}/api/lobby/getRobot`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${localStorage.getItem("userToken")}`
        },
        body: JSON.stringify({
          lobbyID: lobbyId,
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

  /**
   * @author William Pii Jæger
   */
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

  /**
   * @author William Pii Jæger
   */
  const handleStartProgramming = () => {
    sendMessage({
      lobbyID: lobbyId,
      payload: {
        type: "startProgramming",
        windowMs: 60000
      }
    });
  };

  /**
   * @author William Pii Jæger
   */
  const formatTimeRemaining = (ms: number): string => {
    const seconds = Math.ceil(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  /**
   * @author William Pii Jæger
   */
  const getReadinessDisplay = (): string => {
    if (!readiness) return '';
    const submitted = Object.values(readiness.playerSubmitted).filter(Boolean).length;
    const total = Object.keys(readiness.playerSubmitted).length;
    return `${submitted}/${total} players ready`;
  };

  /**
   * @author Kajsa Alice Ulrika Berlstedt
   */
  const renderRobotLabels = (): React.ReactNode => {
    const username = localStorage.getItem("username") || "";
    const entries = Object.entries(robotMap);
    if (!robotID || entries.length === 0) {
      return <div className="player-info">No players are assigned yet</div>;
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
                <>You are: <span className="robot-label" style={{ color }}>{`Robot ${idStr}`}</span></>
              ) : (
                <>{name}: <span className="robot-label" style={{ color }}>{`Robot ${idStr}`}</span></>
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

      <RespawnDirectionModal
        isOpen={needsRespawn}
        onSelectDirection={handleRespawnDirection}
      />

      {reactionPopup && (
        <ReactionPopUp
          reactionKind={reactionPopup.kind}
          options={reactionPopup.options}
          onClose={() => {
            setReactionPopup(null);
          }}
          onSelect={(option) => {
            console.log("Submitting reaction choice:", option);
            sendMessage({
              lobbyID: lobbyId,
              playerID: parseInt(robotID),
              payload: {
                type: "submitReaction",
                choice: option,
              },
            });
            setReactionPopup(null);
          }}
        />
      )}

      <div className="board-Left">
        {mapDisplayName && (
          <div className="map-name-banner">
            <span className="map-label">Map:</span>
            <span className="map-name">{mapDisplayName}</span>
          </div>
        )}
        <div className="boardContainer">
          <BoardRenderer gameData={gameData} startingAreaInfo={startingAreaInfo} />
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
            discard={discardData?.discard || []}
            hand={handData?.hand || []} />
        </div>
      </div>
    </div>
  );
}