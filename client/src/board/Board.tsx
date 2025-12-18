import React, { useState, useEffect, useRef } from "react";
import { Menu, X, Home } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage } from "../utils/ws";
import { MoveType, GameData, HandData, ROBOT_COLORS, DiscardData } from "../types/boardTypes";
import { WinnerBanner } from "./WinnerBanner";
import { BoardRenderer } from "./BoardRenderer";
import { MoveSelector } from "./MoveSelector";
import ReactionPopUp from "./actionSelector";
import CheckpointChecklist from "../ui/checkpointChecklist";
import { leaveLobby } from "../lobby/LeaveLobby";
import { RespawnDirectionModal } from "../ui/RespawnDirectionModal";
import "../styles/gameview.css";
import "../styles/cards.css";
import "../styles/boardelements.css";

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
  const [userID] = useState<string>(sessionStorage.getItem("userID") || "");
  const [lobbyId] = useState<string>(sessionStorage.getItem("id") || "");
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

  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
  const [robotMap, setRobotMap] = useState<{ [username: string]: string }>({});
  const [mapDisplayName, setMapDisplayName] = useState<string>("");
  const [needsRespawn, setNeedsRespawn] = useState<boolean>(false);
  const [respawnRobotId, setRespawnRobotId] = useState<number | null>(null);
  const [firstSubmissionDelayed, setFirstSubmissionDelayed] = useState(false);
  const [moveHistory, setMoveHistory] = useState<{ robotId: number; move: string }[]>([]);
  const moveHistoryRef = useRef<HTMLDivElement | null>(null);
  const readinessIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const [startingAreaInfo, setStartingAreaInfo] = useState<{
    direction: string;
    width: number;
    height: number;
  } | null>(null);
  const [damageDecks, setDamageDecks] = useState<{
    spamCount: number;
    trojanHorseCount: number;
    wormCount: number;
  } | null>(null);

  const [reactionPopup, setReactionPopup] = useState<{
    kind: string;
    options: string[];
    deadline?: number;
  } | null>(null);

  useEffect(() => {
    if (moveHistoryRef.current) {
      moveHistoryRef.current.scrollTop = moveHistoryRef.current.scrollHeight;
    }
  }, [moveHistory]);
  const [winner, setWinner] = useState<number | null>(null);

  const [showExitConfirmation, setShowExitConfirmation] = useState<boolean>(false);

  const isDemoMode = sessionStorage.getItem("mode") == "demo";

  // Fetch lobby info and full board template for Map Banner and starting area info
  useEffect(() => {
    const fetchLobbyInfo = async () => {
      try {
        const userToken = sessionStorage.getItem("userToken");
        const response = await fetch(API_BASE_URL + "/api/lobby/lobbyInfo", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`

          },
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
                Authorization: `Bearer ${sessionStorage.getItem("userToken")}`,
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
                "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
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
        }

        if (data.meta?.game?.winner != null) {
          setWinner(data.meta.game.winner);
          setGameState('finished');
        }

        switch (actualData.type) {
          //check if readiness polling is running
          case "stateSnapshot":
            setGameData(actualData.payload || actualData);
            break;

          case "hand":
            const handPayload = actualData.payload;
            setHandData(handPayload);
            break;

          case "readiness":
            setReadiness(data.payload);
            setTimeRemaining(data.payload.msRemaining);
            break;

          case "discard":
            const discardPayload = actualData.payload;
            setDiscardData(discardPayload);
            break;

          case "timeRemaining":
            setTimeRemaining(data.payload.ms);
            break;

          case "programmingStarted":
            setFirstSubmissionDelayed(false);
            setGameState('programming');
            setHasSubmitted(false);
            sendMessage({ lobbyID: lobbyId, payload: { type: "getDiscard" } });
            sendMessage({ lobbyID: lobbyId, payload: { type: "getDamageDecks" } });
            sendMessage({ lobbyID: lobbyId, payload: { type: "getHand" } });

            startReadinessPolling();
            break;

          case "playerSubmitted":
            sendMessage({ lobbyID: lobbyId, payload: { type: "getReadiness" } });
            break;

          case "roundExecuting":
            setMoveHistory([]);
            setGameState('executing');
            stopReadinessPolling();

            setSelectedMoves(Array(5).fill(null));
            setHasSubmitted(false);
            break;

          case "update":
            sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
            sendMessage({ lobbyID: lobbyId, payload: { type: "getLastMoves" } });
            window.dispatchEvent(new Event('roundExecuted'));
            break;

          case "gameFinished":
            if (actualData.payload?.winner != null) {
              setWinner(actualData.payload.winner);
            }
            setGameState('finished');
            stopReadinessPolling();
            sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
            break;

          case "reactionNeeded":
            setGameState("reaction");
            if (actualData.payload?.kind && actualData.payload?.options) {
              setReactionPopup({
                kind: actualData.payload.kind,
                options: actualData.payload.options,
                deadline: actualData.payload.deadline
              });
            }
            break;


          case "ack":
            if (data.payload.message === "Program submitted") {
              setHasSubmitted(true);
            }
            break;

          case "error":
            console.error("Server error:", data.payload);
            alert(`Error: ${data.payload.message}`);
            break;

          case "needRespawnDirection":
            const deadRobotId = actualData.payload?.robotId;
            const currentRobotId = parseInt(robotID);

            if (deadRobotId === currentRobotId) {
              setNeedsRespawn(true);
              setRespawnRobotId(deadRobotId);
            }
            break;

          case "damageDecks":
            setDamageDecks(actualData.payload);
            break;

          case "lastMoves":

            const raw = actualData.payload?.lastMoves;

            if (typeof raw === "string") {
              const trimmed = raw.slice(1, -1);

              const parsed = trimmed
                .split(", ")
                .map((entry: string) => {
                  const [robotId, move] = entry.split("=");
                  return {
                    robotId: Number(robotId),
                    move: move,
                  };
                });

              setMoveHistory(parsed);
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
    sendMessage({ lobbyID: lobbyId, payload: { type: "getDamageDecks" } });
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
   * @author William Pii Jæger
   */
  const handleForceStartRound = () => {
    if (!isDemoMode) return; // only allow in demo mode

    sendMessage({
      lobbyID: lobbyId,
      payload: {
        type: "forceStartRound",
      },
    });
  };


  /**
   * @author Kajsa Alice Ulrika Berlstedt
   * @author Asger Allin Jensen
   */
  const getRobotIDS = async () => {

    try {
      const response = await fetch(`${API_BASE_URL}/api/lobby/getRobot`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("userToken")}`,
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
      setRobotMap(data);

      const username = sessionStorage.getItem("username");
      if (!username) {
        console.warn("No username found in sessionStorage");
        return;
      }

      const id = data[username];

      if (id) {
        setRobotID(id.toString());
        sessionStorage.setItem("robotID", id.toString()); // optional persistence
      } else {
        console.warn(`No robot ID found for username "${username}" in `, data);
      }
    } catch (error) {
      console.error("Error fetching robot ID:", error);
    }
  };

  /**
   * @author William Pii Jæger
   */
  const handleSubmitMove = (moves: MoveType[]) => {
    if (gameState === "waiting") {
      handleStartProgramming();
    }

    if (hasSubmitted) {
      alert("You have already submitted for this round");
      return;
    }

    const submit = () => {
      sendMessage({
        lobbyID: lobbyId,
        payload: {
          type: "submitProgram",
          cards: moves
        }
      });
    };

    if (!firstSubmissionDelayed) {
      setFirstSubmissionDelayed(true);
      setTimeout(submit, 200);
    } else {
      submit();
    }
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

  useEffect(() => {
    if (gameState !== 'programming') return;

    if (hasSubmitted) return;

    if (isDemoMode) return;

    if (timeRemaining <= 2000 && timeRemaining > 0) {
      const hasAnyMoves = selectedMoves.some(move => move !== null);

      if (hasAnyMoves) {
        console.log('Auto-submitting due to time running out...');

        const movesToSubmit = selectedMoves.filter(move => move !== null) as MoveType[];

        if (movesToSubmit.length > 0) {
          handleSubmitMove(movesToSubmit);
        }
      }
    }
  }, [timeRemaining, gameState, hasSubmitted, selectedMoves]);

  /**
   * @author William Pii Jæger
   */
  const formatTimeRemaining = (ms: number): string => {
    const seconds = Math.ceil(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')} `;
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
    const username = sessionStorage.getItem("username") || "";
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
      {winner != null && (
        <WinnerBanner winnerId={winner} />
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

      {showExitConfirmation && (
        <div className="confirmation-overlay">
          <div className="confirmation-modal">
            <div className="confirmation-content">
              <p className="confirmation-text">
                ARE YOU SURE YOU WANT TO EXIT TO THE MAIN MENU?
                <br />
                THE GAME WILL CONTINUE WITHOUT YOU.
              </p>
            </div>
            <div className="confirmation-buttons">
              <button
                className="metal-button icon"
                onClick={() => {
                  leaveLobby(lobbyId, sessionStorage.getItem("userID"), null);
                  navigate("/lobbyScene");
                }}
                aria-label="Yes, exit game"
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>YES</span>
              </button>
              <button
                className="metal-button icon"
                onClick={() => setShowExitConfirmation(false)}
                aria-label="No, stay in game"
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>NO</span>
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="board-top-row">
        <div className="board-top-banner">
          <div className="map-banner-wrapper">
            <button
              className="go-home-btn"
              onClick={() => setShowExitConfirmation(true)}
              aria-label="Go to homepage"
            >
              <Home size={20} />
            </button>

            {mapDisplayName && (
              <div className="map-banner-content">
                <span className="map-label">Map:</span>
                <span className="map-name">{mapDisplayName}</span>
              </div>
            )}
          </div>

          <div className="discard-pile-wrapper">
            <h3 className="discard-title">Discard Pile ({discardData?.discard?.length || 0})</h3>
            <div className="discard-cards-grid">
              {discardData?.discard && discardData.discard.length > 0 ? (
                discardData.discard.map((card, index) => (
                  <span key={index} className="discard-card-chip">{card}</span>
                ))
              ) : (
                <span className="discard-empty-msg">No cards in discard pile</span>
              )}
            </div>
          </div>
        </div>

        <div className="board-main-content">
          <div className="board-left-column">
            <div className="board-box-wrapper">
              <div className="boardContainer">
                <BoardRenderer gameData={gameData} startingAreaInfo={startingAreaInfo} />
              </div>
            </div>
          </div>

          <div className="board-right-column">
            <div className="controls-wrapper">
              <MoveSelector
                moves={handData?.hand || []}
                selectedMoves={selectedMoves}
                onChange={setSelectedMoves}
                onSubmitMove={handleSubmitMove}
                hasEmptySlots={selectedMoves.some((m) => m === null)}
                isDemoMode={!!isDemoMode}
                onForceStartRound={handleForceStartRound}
                hasSubmitted={hasSubmitted}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="board-bottom-row">
        <div className="game-info-box">
          <div className="info-content">
            <div className="info-section">
              <div className="info-section-title">Players</div>
              {renderRobotLabels()}
            </div>

            <div className="info-section">
              <div className="info-section-title">Last Moves</div>

              {moveHistory.length > 0 ? (
                <div
                  className="last-move-content"
                  ref={moveHistoryRef}
                  style={{ maxHeight: "150px", overflowY: "auto" }}
                >
                  {moveHistory.map((move, index) => (
                    <div key={index}>
                      <p className="last-move-robot">
                        <span
                          style={{
                            color: ROBOT_COLORS[(move.robotId - 1) % ROBOT_COLORS.length],
                            fontWeight: "bold",
                          }}
                        >
                          Robot {move.robotId}
                        </span>
                      </p>
                      <p className="last-move-action">{move.move}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="last-move-empty">No moves yet</p>
              )}
            </div>

            {gameState === 'programming' && readiness && (
              <div className="info-section">
                <div className="info-section-title">Player Status</div>
                <ul className="readiness-list">
                  {Object.entries(readiness.playerSubmitted).map(([playerId, submitted]) => (
                    <li key={playerId}>
                      Player {playerId}: {submitted ? '✓ Submitted' : '⏳ Waiting'}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>

        <div className="stats-info-box">
          <div className="info-content">
            <div className="info-section">
              <div className="info-section-title">Damage Cards Left</div>
              {damageDecks && (
                <div className="damage-decks-info">
                  <p>Spam: {damageDecks.spamCount} </p>
                  <p>Trojan Horse: {damageDecks.trojanHorseCount} </p>
                  <p>Worm: {damageDecks.wormCount} </p>
                </div>
              )}
            </div>

            <div className="info-section">
              <div className="info-section-title">Game State</div>
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
                      ✓ You have submitted your registers
                    </div>
                  )}
                </>
              )}

              {gameState === 'executing' && (
                <div className="executing-message">
                  Round is executing...
                </div>
              )}
            </div>

            <div className="info-section">
              <div className="info-section-title">Checkpoints</div>
              {gameData && (
                <CheckpointChecklist board={gameData.board} robots={gameData.robots} />
              )}
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}
