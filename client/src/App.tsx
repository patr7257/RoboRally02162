/*
Author(s): Asger, Bjarke, Patrick
*/

import './App.css';
import LoginComp from "./lobby/LoginComp";
import RegisterComp from "./lobby/RegisterComp";
import { BrowserRouter as Router, Routes, Route, useNavigate } from "react-router-dom";
import Board from "./board/Board";
import Lobby from "./lobby/Lobby";
import React, { useState } from "react";
import Layout from "./lobby/Layout";
import { closeSocket } from "./utils/ws";
import JoinLobby from './lobby/JoinLobby';
import LobbyCreation from './lobby/LobbyCreation';
import "./ui/registerEffects";

declare global {
  interface Window {
    __appStarted__?: boolean;
  }
}

function Home() {
  const navigate = useNavigate();
  const [username, setUsername] = useState<string>(localStorage.getItem("username") || "");

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    setUsername("");
    closeSocket();
  };

  const handleLobby = () => {
    navigate("/lobbyScene");
  };

  const handleLoggedIn = (name: string) => {
    setUsername(name);
  };

  return (
    <Layout>
      <h1>Welcome to RoboRally!</h1>
      {username ? (
        <div className="home-user-panel">
          <p className="home-username">Logged in as: <strong>{username}</strong></p>
          <div className="home-actions">
            <button className="big-button wide-button" onClick={handleLobby}>
              Make or join lobby
            </button>
            <button className="big-button wide-button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </div>
      ) : (
        <div className="auth-container">
          <LoginComp onLogin={setUsername} />
          <RegisterComp />
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
    localStorage.removeItem("lobbies");
    window.__appStarted__ = true;
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/boardScene" element={<Board />} />
        <Route path="/lobbyScene" element={<Lobby />} />
        <Route path="/lobbyJoinScene" element={<JoinLobby />} />
        <Route path="/lobbyCreationScene" element={<LobbyCreation />} />
      </Routes>
    </Router>
  );
}

export default App;
