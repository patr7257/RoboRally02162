/*
Author(s): Bjarke
*/

import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import React, { useState } from "react";

export default function LobbyCreation() {
    const navigate = useNavigate();
    const usernameInput = localStorage.getItem("username");
    const [lobbyId, setLobbyId] = useState(localStorage.getItem("id") || "");
    const [error, setError] = useState("");

    return (
        <Layout>
            <h1>Lobby Creation</h1>
            <p>Your lobby ID is: {lobbyId}</p>
            <div style={{
                marginTop: "50px",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "12px"
            }}>
        
                    
                

                <button className="big-button">
                    Start Game
                </button>

                {/* do something so that it disconnects from the lobby */}
                <button className="big-button" onClick={() => navigate("/lobbyScene")}>
                    Go back to lobby menu
                </button>
            </div>
        </Layout>
    );


}