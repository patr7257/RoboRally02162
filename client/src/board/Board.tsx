import React, { useState, useEffect, useCallback, useRef, useMemo } from "react";
import { Home } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage, closeSocket, getRoster, getMyRobotId, getMyRole, getBoardId } from "../utils/ws";
import { MoveType, GameData, HandData, ROBOT_COLORS, DiscardData } from "../types/boardTypes";
import { PostGamePanel } from "./WinnerBanner";
import { BoardRenderer } from "./BoardRenderer";
import { MoveSelector } from "./MoveSelector";
import ReactionPopUp from "./actionSelector";
import CheckpointChecklist from "../ui/checkpointChecklist";
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
  const locked =
    gameState === "executing" ||
    gameState === "finished" ||
    gameState === "reaction" ||
    hasSubmitted;
  const [robotID, setRobotID] = useState<string>(getMyRobotId());

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

  const [boardId, setBoardId] = useState<string | null>(null);
  const [errorBanner, setErrorBanner] = useState<{ message: string; key: number } | null>(null);
  // Host handoff status (issue #4): stale host, election outcome, own demotion.
  const [hostBanner, setHostBanner] = useState<string | null>(null);

  useEffect(() => {
    if (moveHistoryRef.current) {
      moveHistoryRef.current.scrollTop = moveHistoryRef.current.scrollHeight;
    }
  }, [moveHistory]);
  const [winner, setWinner] = useState<number | null>(null);
  // Only the acting host tab can PUT the rematch state (issue #10).
  const isHost = getMyRole() === "host";

  const [showExitConfirmation, setShowExitConfirmation] = useState<boolean>(false);

  const localSubmitLatchRef = useRef(false);

  const isDemoMode = sessionStorage.getItem("mode") === "demo";

  const canForceStartRound = useMemo(() => {
    if (!isDemoMode || gameState !== 'programming' || !hasSubmitted || !readiness) {
      return false;
    }

    const hasUnsubmittedPlayers = Object.values(readiness.playerSubmitted).some(
      (submitted) => !submitted
    );

    return hasUnsubmittedPlayers;
  }, [isDemoMode, gameState, hasSubmitted, readiness]);


  /**
  * @author Weihao Mo
  */
  const robotIdToUsername = useMemo(() => {
    const map: Record<number, string> = {};
    Object.entries(robotMap).forEach(([username, robotId]) => {
      const id = Number(robotId);
      if (!isNaN(id)) {
        map[id] = username;
      }
    });
    return map;
  }, [robotMap]);

  /**
   * @author William Pii Jæger
   */
  const stopReadinessPolling = useCallback(() => {
    if (readinessIntervalRef.current) {
      clearInterval(readinessIntervalRef.current);
      readinessIntervalRef.current = null;
    }
  }, []);

  /**
   * @author William Pii Jæger
   */
  const startReadinessPolling = useCallback(() => {
    stopReadinessPolling();
    readinessIntervalRef.current = setInterval(() => {
      sendMessage({ lobbyID: lobbyId, payload: { type: "getReadiness" } });
    }, 1000);
  }, [lobbyId, stopReadinessPolling]);

  // Map banner + starting-area shading come from the bundled board definition.
  // Prefer the board actually selected for this game (boards/<id>.json); fall
  // back to the legacy single board.json when the id is not yet known or the
  // per-board file does not exist.
  useEffect(() => {
    let cancelled = false;

    const applyDef = (def: any) => {
      if (cancelled || !def) return;
      setMapDisplayName(def.displayName || "Robot Rally");
      if (def.startingBoardDirection) {
        setStartingAreaInfo({
          direction: String(def.startingBoardDirection).toUpperCase(),
          width: def.startingBoardWidth || 0,
          height: def.startingBoardHeight || 0,
        });
      }
    };

    const fetchFallback = () =>
      fetch(`${process.env.PUBLIC_URL}/board.json`)
        .then((r) => (r.ok ? r.json() : null))
        .then(applyDef)
        .catch(() => {
          /* board.json is optional cosmetic metadata */
        });

    if (boardId) {
      fetch(`${process.env.PUBLIC_URL}/boards/${boardId}.json`)
        .then((r) => (r.ok ? r.json() : Promise.reject(new Error("board json not found"))))
        .then(applyDef)
        .catch(() => {
          if (!cancelled) fetchFallback();
        });
    } else {
      fetchFallback();
    }

    return () => {
      cancelled = true;
    };
  }, [boardId]);

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
    let firstProgramming: number = 0;
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
            setBoardId(getBoardId());
            break;

          case "hand":
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
            const discardPayload = actualData.payload;
            setDiscardData(discardPayload);
            break;

          case "timeRemaining":
            setTimeRemaining(data.payload.ms);
            break;

          case "programmingStarted":
            localSubmitLatchRef.current = false;
            setFirstSubmissionDelayed(false);
            setGameState('programming');
            setHasSubmitted(false);
            // A rematch (issue #10) re-enters programming on a finished game;
            // every tab leaves the post-game panel without a reload.
            setWinner(null);

            startReadinessPolling();

            if (firstProgramming === 0) {
              firstProgramming++;
              break;
            }
            sendMessage({ lobbyID: lobbyId, payload: { type: "getDiscard" } });
            sendMessage({ lobbyID: lobbyId, payload: { type: "getDamageDecks" } });
            sendMessage({ lobbyID: lobbyId, payload: { type: "getHand" } });

            break;

          case "playerSubmitted":
            sendMessage({ lobbyID: lobbyId, payload: { type: "getReadiness" } });
            break;

          case "roundExecuting":
            localSubmitLatchRef.current = false;
            setMoveHistory([]);
            setGameState('executing');
            stopReadinessPolling();
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
              localSubmitLatchRef.current = true;
            }
            break;

          case "error":
            console.error("Server error:", actualData.payload);
            setErrorBanner({
              message: actualData.payload?.message || "An error occurred",
              key: Date.now(),
            });
            break;

          case "hostStale":
            setHostBanner("Host connection lost, electing a new host...");
            break;

          case "hostRecovered":
            setHostBanner(null);
            break;

          case "hostDemoted":
            setHostBanner("You are no longer the host");
            break;

          case "hostChanged":
            setHostBanner("You are now the host");
            break;

          case "needRespawnDirection":
            const deadRobotId = actualData.payload?.robotId ?? actualData.payload?.deadRobotId;
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
            const moves = actualData.payload?.moves;
            if (Array.isArray(moves)) setMoveHistory(moves);
            break;

          default:
            break;
        }
      } catch (e) {
        console.log("Raw text message:", message);
      }
    });

    sendMessage({ lobbyID: lobbyId, payload: { type: "getBoard" } });
    sendMessage({ lobbyID: lobbyId, payload: { type: "getDiscard" } });
    sendMessage({ lobbyID: lobbyId, payload: { type: "getDamageDecks" } });
    sendMessage({ lobbyID: lobbyId, payload: { type: "getHand" } });

    refreshRoster();

    return () => {
      stopReadinessPolling();
      if (unsubscribe) unsubscribe();
    };
  }, [lobbyId, robotID, startReadinessPolling, stopReadinessPolling]);

  // Roster arrives with the authoritative snapshot; refresh once it loads.
  useEffect(() => {
    refreshRoster();
  }, [gameData]);

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

  // Non-blocking error banner: auto-dismiss after 8s.
  useEffect(() => {
    if (!errorBanner) return;
    const timer = setTimeout(() => setErrorBanner(null), 8000);
    return () => clearTimeout(timer);
  }, [errorBanner]);

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
   * Post-game actions (issue #10). "Play again" is host-only (only the
   * acting host tab can PUT the rematch state); "Back to menu" is available
   * to everyone and just leaves the game like the exit-confirmation path.
   */
  const handlePlayAgain = () => {
    sendMessage({ lobbyID: lobbyId, payload: { type: "rematch" } });
  };

  const handleBackToMenu = () => {
    closeSocket(1000);
    navigate("/");
  };


  /**
   * @author Kajsa Alice Ulrika Berlstedt
   * @author Asger Allin Jensen
   */
  const refreshRoster = () => {
    const roster = getRoster();
    if (roster.length > 0) {
      const map: { [username: string]: string } = {};
      roster.forEach((p) => {
        map[p.name] = String(p.robotId);
      });
      setRobotMap(map);
    }
    const mine = getMyRobotId();
    if (mine) {
      setRobotID(mine);
      sessionStorage.setItem("robotID", mine);
    }
  };

  /**
   * @author William Pii Jæger
   */
  const handleStartProgramming = useCallback(() => {
    sendMessage({
      lobbyID: lobbyId,
      payload: {
        type: "startProgramming",
        windowMs: 60000
      }
    });
  }, [lobbyId]);

  /**
   * @author William Pii Jæger
   */
  const handleSubmitMove = useCallback((moves: MoveType[]) => {
    if (gameState === "waiting") {
      handleStartProgramming();
    }

    if (hasSubmitted) {
      alert("You have already submitted for this round");
      return;
    }

    localSubmitLatchRef.current = true;

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
  }, [gameState, hasSubmitted, firstSubmissionDelayed, lobbyId, handleStartProgramming]);

  useEffect(() => {
    if (gameState !== 'programming') return;

    if (hasSubmitted || localSubmitLatchRef.current) return;

    if (isDemoMode) return;

    if (timeRemaining <= 2000 && timeRemaining > 0) {
      const hasAnyMoves = selectedMoves.some(move => move !== null);

      if (hasAnyMoves) {
        const movesToSubmit = selectedMoves.filter(move => move !== null) as MoveType[];

        if (movesToSubmit.length > 0) {
          localSubmitLatchRef.current = true;
          handleSubmitMove(movesToSubmit);
        }
      }
    }
  }, [timeRemaining, gameState, hasSubmitted, selectedMoves, isDemoMode, handleSubmitMove]);

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
   * @author Weihao Mo
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
        <PostGamePanel
          winnerId={winner}
          robotIdToUsername={robotIdToUsername}
          isHost={isHost}
          onPlayAgain={handlePlayAgain}
          onBackToMenu={handleBackToMenu}
        />
      )}

      {errorBanner && (
        <div className="error-banner" role="alert">
          <span className="error-banner-message">{errorBanner.message}</span>
          <button
            className="error-banner-close"
            onClick={() => setErrorBanner(null)}
            aria-label="Dismiss error"
          >
            ×
          </button>
        </div>
      )}

      {hostBanner && (
        <div className="error-banner host-banner" role="status">
          <span className="error-banner-message">{hostBanner}</span>
          <button
            className="error-banner-close"
            onClick={() => setHostBanner(null)}
            aria-label="Dismiss host notice"
          >
            ×
          </button>
        </div>
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
                  closeSocket(1000);
                  navigate("/");
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
                canForceStartRound={canForceStartRound}
                onForceStartRound={handleForceStartRound}
                hasSubmitted={hasSubmitted}
                locked={locked}
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
                  {moveHistory.map((move, index) => {
                    const displayName = robotIdToUsername[move.robotId];
                    return (
                      <div key={index}>
                        <p className="last-move-robot">
                          <span
                            style={{
                              color: ROBOT_COLORS[(move.robotId - 1) % ROBOT_COLORS.length],
                              fontWeight: "bold",
                            }}
                          >
                            {displayName}
                          </span>
                        </p>
                        <p className="last-move-action">{move.move}</p>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="last-move-empty">No moves yet</p>
              )}
            </div>

            {gameState === 'programming' && readiness && (
              <div className="info-section">
                <div className="info-section-title">Player Status</div>
                <ul className="readiness-list">
                  {Object.entries(readiness.playerSubmitted).map(([playerId, submitted]) => {
                    const id = Number(playerId);
                    const displayName = robotIdToUsername[id];
                    return (
                      <li key={playerId}>
                        {displayName}: {submitted ? '✓ Submitted' : '⏳ Waiting'}
                      </li>
                    );
                  })}
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
                <CheckpointChecklist
                  board={gameData.board}
                  robots={gameData.robots}
                  robotIdToUsername={robotIdToUsername}
                />
              )}
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}