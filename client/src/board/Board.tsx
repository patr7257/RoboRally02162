/*
Author(s): Bjarke, Asger, Patrick
*/

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { subscribe, sendMessage } from "../utils/ws";
import { MoveType, GameData } from "../types";
import { WinnerBanner } from "./WinnerBanner";
import { BoardRenderer } from "./BoardRenderer";
import { MoveSelector } from "./MoveSelector";

export default function Board() {
    const navigate = useNavigate();
    const [lobbyId, setLobbyId] = useState<string>(localStorage.getItem("id") || "");
    const [gameData, setGameData] = useState<GameData | null>(null);
    const [selectedMove, setSelectedMove] = useState<MoveType | null>(null);

    useEffect(() => {
        const unsubscribe = subscribe((message: string) => {
            try {
                const data = JSON.parse(message);
                console.log("Parsed data:", data);
                if (data.type === "game") {
                    setGameData(data.payload);
                }
            } catch {
                console.log("Raw text message:", message);
            }
        });
        
        // Initialize game, contacting host
        sendMessage({
            lobbyID: lobbyId,
            payload: { type: "submitProgram", cards: [] },
        });

        return () => {
            if (unsubscribe) unsubscribe();
        };
    }, [lobbyId]);

    const handleSubmitMove = () => {
        if (!selectedMove) return;

        sendMessage({
            lobbyID: lobbyId,
            payload: { type: "submitProgram", cards: [selectedMove] },
        });
    };

    const handleStartRound = () => {
        sendMessage({
            lobbyID: lobbyId,
            payload: { type: "startRound" },
        });
    };

    return (
        <div className="board-root">
            <div className="navigation">
                <h1>Board Scene</h1>
                <button onClick={() => navigate("/")}>Go to homepage</button>
            </div>

            {gameData?.game?.winner != null && (
                <WinnerBanner winnerId={gameData.game.winner} />
            )}

            <BoardRenderer gameData={gameData} />

            <div className="controls">
                <MoveSelector selectedMove={selectedMove} onSelectMove={setSelectedMove} />
                <button onClick={handleSubmitMove} disabled={!selectedMove}>
                    Make Move
                </button>
                <button onClick={handleStartRound}>Start Round</button>
            </div>
        </div>
    );
}