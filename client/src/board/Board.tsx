

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage } from "../utils/ws";
import { MoveType, GameData, HandData } from "../types/boardTypes";
import { WinnerBanner } from "./WinnerBanner";
import { BoardRenderer } from "./BoardRenderer";
import { GameControls } from "./GameControls";
import CheckpointChecklist from "../ui/checkpointChecklist";
import { leaveLobby } from '../lobby/LeaveLobby';

/*
Author(s): Bjarke, Asger, Patrick, William
*/

interface ReadinessData {
  playerSubmitted: Record<number, boolean>;
  msRemaining: number;
}

export default function Board() {
  const navigate = useNavigate();
  const [lobbyId] = useState<string>(localStorage.getItem("id") || "");
  const [gameData, setGameData] = useState<GameData | null>(null);
  const [handData, setHandData] = useState<HandData | null>(null);
  const [selectedMoves, setSelectedMoves] = useState<(MoveType | null)[]>(Array(5).fill(null));
  const [readiness, setReadiness] = useState<ReadinessData | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<number>(0);
  const [gameState, setGameState] = useState<'waiting' | 'programming' | 'executing' | 'finished'>('waiting');
  const [hasSubmitted, setHasSubmitted] = useState<boolean>(false);

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

  return (
    <div className="board-root">
      <div className="navigation">
        <h1>Board Scene</h1>
        <button
          onClick={() => {
            leaveLobby(lobbyId, localStorage.getItem("username"), null);
            navigate("/");
          }}
        >
          Go to homepage
        </button>
      </div>

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
      </div>

      {gameData?.game?.winner != null && (
        <WinnerBanner winnerId={gameData.game.winner} />
      )}

      <BoardRenderer gameData={gameData} />

      <GameControls
        selectedMoves={selectedMoves}
        onSubmitMove={handleSubmitMove}
        onSelectMove={setSelectedMoves}
        hand={handData?.hand || []}
      />
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
  );
}