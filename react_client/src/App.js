/*
Author(s): Asger, Bjarke, Patrick
*/

import './App.css';
import LoginComp from "./LoginComp";
import RegisterComp from "./RegisterComp";
import { BrowserRouter as Router, Routes, Route, useNavigate } from "react-router-dom";
import Board from "./Board";
import React, { useEffect, useState } from "react";
import { getSocket, subscribe } from "./ws";

function Home() {
  const navigate = useNavigate();
  const [username, setUsername] = useState(localStorage.getItem("token") || ""); // load from storage

  const handleLogout = () => {
    localStorage.removeItem("token");
    setUsername("");
  };

  return (
    <div className='App'>
      <header className='App-header'>
        <img
          src="/roborallyLogo.webp"
          alt="RoboRally Logo"
          style={{ width: 600, marginBottom: 20 }}
        />
        <h1>Welcome to the Robo Rally React Client</h1>

        {/* Display username if logged in */}
        {username ? (
          <div style={{ marginBottom: 20 }}>
            <p style={{ fontSize: "1.2rem" }}>Logged in as: <strong>{username}</strong></p>
            <button className="big-button" onClick={handleLogout}>Logout</button>
          </div>
        ) : (
          // Show login and register forms if not logged in
          <div className="auth-container" style={{ display: "flex", gap: "40px", marginTop: 40 }}>
            <LoginComp onLogin={setUsername} />
            <RegisterComp onRegister={setUsername} />
          </div>
        )}

        <div className="button-container">
          <GameLogic />
          <button
            className="big-button"
            onClick={() => navigate("/boardScene")}
          >
            Go to Board
          </button>
        </div>
      </header>
    </div>
  );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/boardScene" element={<Board />} />
      </Routes>
    </Router>
  );
}

function GameLogic() {
  useEffect(() => {
    const socket = getSocket();
    const unsubscribe = subscribe((message) => {
      console.log("Received game message:", message);
    });
    return () => unsubscribe();
  }, []);
  return null;
}

export default App;