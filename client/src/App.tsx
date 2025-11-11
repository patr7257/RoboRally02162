/*
Author(s): Asger, Bjarke, Patrick, Lizette
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
import LobbyCreator from './lobby/LobbyCreator';
import LobbyCreation from './lobby/LobbyCreation';
import "./ui/registerEffects";
import LoadLobby from './lobby/LoadLobby';

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
    localStorage.removeItem("userID");
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
      <div className="panel-container">
        <h1 className="metal-text">Welcome to RoboRally</h1>
        <h2 className="home-username">Command Access Portal</h2>

        {username ? (
          <>
            <p className="home-username">
              Logged in as: <strong>{username}</strong>
            </p>

            <div className="control-panel">
              <button className="metal-button" onClick={handleLobby}>
                Enter Command Center
              </button>

              <button className="metal-button" onClick={handleLogout}>
                Logout
              </button>
            </div>
          </>
        ) : (
          <div className="control-panel auth-panel">
            <LoginComp onLogin={handleLoggedIn} />
            <RegisterComp />
          </div>
        )}
      </div>
      {/* 🔩 evenly spaced screws along both sides */}
      {Array.from({ length: 10 }).map((_, i) => (
        <div
          key={`screw-left-${i}`}
          className="screw screw-left"
          style={{ top: `${5 + (i / 9) * 90}vh` }}
        ></div>
      ))}

      {Array.from({ length: 10 }).map((_, i) => (
        <div
          key={`screw-right-${i}`}
          className="screw screw-right"
          style={{ top: `${5 + (i / 9) * 90}vh` }}
        ></div>
      ))}
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
        <Route path="/lobbyCreatorScene" element={<LobbyCreator />} />
        <Route path= "/lobbyCreationScene" element={<LobbyCreation />}/>
        <Route path="/lobbyLoadScene" element={<LoadLobby />} />
      </Routes>
    </Router>
  );
}

export default App;