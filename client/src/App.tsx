import './App.css';
import LoginComp from "./lobby/LoginComp";
import RegisterComp from "./lobby/RegisterComp";

import { BrowserRouter as Router, Routes, Route, useNavigate } from "react-router-dom";
import Board from "./board/Board";
import Lobby from "./lobby/Lobby";
import React, { useState,useEffect } from "react";
import Layout from "./lobby/Layout";
import { closeSocket } from "./utils/ws";
import JoinLobby from './lobby/JoinLobby';
import LobbyCreator from './lobby/LobbyCreator';
import LobbyCreation from './lobby/LobbyCreation';
import "./ui/registerEffects";
import LoadLobby from './lobby/LoadLobby';
import useSound from 'use-sound';
import SeeAllGames from './lobby/SeeAllGames';
import UserSettings from './lobby/UserSettings';
import LoadDemo from './lobby/LoadDemo';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 * @author Lizette Bloch Dahl Nikolajsen
 */
declare global {
  interface Window {
    __appStarted__?: boolean;
  }
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 * @author Lizette Bloch Dahl Nikolajsen
 */
function Home() {
  const navigate = useNavigate();
  const [username, setUsername] = useState<string>(sessionStorage.getItem("username") || "");
  const [error, setError] = useState<string>("");

  useEffect(()=> {
    const reason : String | null = sessionStorage.getItem("returnReason");
    if (reason) {
      setError(""+reason);
      sessionStorage.removeItem("returnReason");
    }
  })

  const handleLogout = async () => {
    const userID = sessionStorage.getItem("userID");

    if (userID) {
      try {
        const response = await fetch(
          API_BASE_URL + "/api/users/logout?userID=" + userID,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "Authorization": `Bearer ${sessionStorage.getItem("userToken")}`
            }
          }
        );

        if (!response.ok) {
          console.error("Logout failed with status:", response.status);
        }
      } catch (err) {
        console.error("Logout request error:", err);
      }
    }

    // Always clear frontend session, even if backend fails
    sessionStorage.removeItem("token");
    sessionStorage.removeItem("username");
    sessionStorage.removeItem("userID");

    setUsername("");
    closeSocket(1000);
  };

  const handleLobby = () => {
    navigate("/lobbyScene");
  };

  const handleLoggedIn = (name: string) => {
    setUsername(name);
  };
  const clearError = ():void=> {
    setError("");
  };
  return (
    <Layout>
      <div className="panel-container" onClick={clearError} onKeyDown={clearError}>
        <h1 className="metal-text easteregg">Welcome to RoboRally</h1>
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
         {error && <p className="error-text">{error}</p>}
      </div>
      {/* evenly spaced screws along both sides */}
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
  const easter_egg = document.querySelector(".easteregg");
  const [play] = useSound('./utils/roborawy.mp3');
  easter_egg?.addEventListener("click", (e) => {
    play();
  });

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/boardScene" element={<Board />} />
        <Route path="/lobbyScene" element={<Lobby />} />
        <Route path="/lobbyJoinScene" element={<JoinLobby />} />
        <Route path="/lobbyCreatorScene" element={<LobbyCreator />} />
        <Route path="/lobbyCreationScene" element={<LobbyCreation />} />
        <Route path="/lobbyLoadScene" element={<LoadLobby />} />
        <Route path="/seeAllGamesScene" element={<SeeAllGames />} />
        <Route path="/userSettings" element={<UserSettings />} />
        <Route path="/demoLoadScene" element={<LoadDemo />} />

      </Routes>
    </Router>
  );
}

export default App;