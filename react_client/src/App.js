/*
Author(s): Asger, Bjarke, Patrick
*/

import './App.css';
import LoginComp from "./LoginComp";
import RegisterComp from "./RegisterComp";
import { BrowserRouter as Router, Routes, Route, useNavigate } from "react-router-dom";
import Board from "./Board";
import Lobby from "./Lobby";
import React, { useEffect, useState} from "react";
import Layout from "./Layout";
import { closeSocket } from "./ws";
import JoinLobby from './JoinLobby';
import PreGame from './PreGame';
import LobbyCreation from './LobbyCreation';

function Home() {

  const navigate = useNavigate();
  const [username, setUsername] = useState(localStorage.getItem("token") || ""); // load from storage

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    setUsername("");
    closeSocket();
  };

  const handleLobby = () => {
    navigate("/lobbyScene");
  };

  return (
    <Layout>
      <h1>Welcome to the Robo Rally React Client</h1>

      {/* Display username if logged in */}
      {username ? (
        <div style={{ marginBottom: 20 }}>
          <p style={{ fontSize: "1.2rem" }}>Logged in as: <strong>{username}</strong></p>
          <div style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",   
            gap: "12px"            
          }}>
            <button className="big-button" onClick={handleLobby} style={{ width: "200px" }}>
              Make or join lobby
            </button>
            <button className="big-button" onClick={handleLogout} style={{ width: "200px" }}>
              Logout
            </button>
          </div>
        </div>

      ) : (
        // Show login and register forms if not logged in
        <div className="auth-container" style={{ display: "flex", gap: "40px", marginTop: 40 }}>
          <LoginComp onLogin={setUsername} />
          <RegisterComp onRegister={setUsername} />
        </div>
      )}
    </Layout>
  );
}

function App() {

  if (!window.__appStarted__) {
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("id");
  localStorage.removeItem("lobbies")
  window.__appStarted__ = true; // flag to prevent running again
  }


  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/boardScene" element={<Board />} />
        <Route path="/lobbyScene" element={<Lobby />} />
        <Route path="/lobbyJoinScene" element={<JoinLobby />} />
        <Route path="/LobbyCreationScene" element={<LobbyCreation />} />
        <Route path="/preGameScene" element={<PreGame />} />
      </Routes>
    </Router>
  );
}
export default App;