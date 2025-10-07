/*
Author(s): Lizette, Asger
*/

import React, { useState } from "react";
import { getSocket, subscribe } from "./ws";
import { sha256Hex } from "./hashPassword";

interface LoginCompProps {
  onLogin: (username: string) => void;
}

export default function LoginComp({ onLogin }: LoginCompProps) {
  const [usernameInput, setUsernameInput] = useState<string>("");
  const [passwordInput, setPasswordInput] = useState<string>("");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;


  const handleLogin = async () => {
    setError("");
    try {
      const clientHash = await sha256Hex(passwordInput);

      const response = await fetch(API_BASE_URL+"/api/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: usernameInput,
          passwordHash: clientHash,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem("token", data.token);
        localStorage.setItem("username", usernameInput);
        console.log("pass: " + usernameInput);
        await getSocket();
        subscribe((message: string) => {
          console.log("Received game message:", message);
        });

        onLogin(data.username ?? usernameInput);
      } else if (response.status === 401) {
        setError("Login failed. Invalid username or password.");
      } else {
        setError("An error occurred. Try again.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Try again.");
    }
  };

  return (
    <div className="login-container">
      <h2>Login</h2>

      <div>
        <input
          type="text"
          placeholder="Enter username"
          value={usernameInput}
          onChange={(e) => setUsernameInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleLogin()}
        />
      </div>

      <div>
        <input
          type="password"
          placeholder="Enter password"
          value={passwordInput}
          onChange={(e) => setPasswordInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleLogin()}
        />
      </div>

      <button type="button" onClick={handleLogin}>
        Login
      </button>
      {error && <p style={{ color: "red" }}>{error}</p>}
    </div>
  );
}
