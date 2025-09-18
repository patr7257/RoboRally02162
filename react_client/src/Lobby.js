/*
Author(s): Bjarke
*/


import React, { useState, useEffect} from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";

export default function Lobby() {
    const navigate = useNavigate();
    const usernameInput = localStorage.getItem("username");
    const [lobbyId, setLobbyId] = useState(localStorage.getItem("id") || "");
    const [error, setError] = useState("");

    useEffect(() => {
        setLobbyId("");
        setError("");
    }, []);

    const createLobby = async () => {
        setError("");
        try {
            const response = await fetch("http://localhost:8080/api/lobby/create", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username: usernameInput }),
            });

            const data = await response.text();
            console.log("data recived: " + data)
            if (response.status === 201) {
                localStorage.setItem("id", data);
                setLobbyId(data);
            } else {
                setError("An error occurred. Try again.");
            }
        } catch (err) {
            console.error("Login error:", err);
            setError("Network error. Try again.");
        }
    };


    return (

        <Layout>
            <h1>Lobby menu</h1>
            <div style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",   
            gap: "12px" }}>
            <button className="big-button" onClick={async () => {await createLobby(); navigate("/lobbyCreationScene")}}>
                Create Lobby
            </button>
            <button className="big-button" onClick={() => navigate("/lobbyJoinScene")}>
                Join Lobby
            </button>
            <button className="big-button" onClick={() => navigate("/")}>
                Go to homepage
            </button>
            </div>
        </Layout>
    );
}
