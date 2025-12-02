import React, { useState } from "react";
import { getSocket, subscribe } from "../utils/ws";
import { sha256Hex } from "../utils/hashPassword";

/**
 * @author Asger Allin Jensen
 * @author Lizette Bloch Dahl Nikolajsen
 */

interface LoginCompProps {
  onLogin: (username: string) => void;
}

/**
 * @author Asger Allin Jensen
 * @author Lizette Bloch Dahl Nikolajsen
 */

export default function LoginComp({ onLogin }: LoginCompProps) {
  const [usernameInput, setUsernameInput] = useState<string>("");
  const [passwordInput, setPasswordInput] = useState<string>("");
  const [error, setError] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;


  const handleLogin = async () => {
    setError("");
    try {
      const clientHash = await sha256Hex(passwordInput);

      const response = await fetch(API_BASE_URL + "/api/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: usernameInput,
          passwordHash: clientHash,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        sessionStorage.setItem("userToken", data.token);
        sessionStorage.setItem("username", usernameInput);
        sessionStorage.setItem("userID", data.userID);
        console.log("userID: " + data.userID);
        console.log("pass: " + usernameInput);
        await getSocket("LOGIN");
        subscribe((message: string) => {
            // I remove the print statement, it was annoying
        });

        onLogin(data.username ?? usernameInput);
      } else if (response.status === 401) {
        setError("Login failed. Invalid username or password.");
      } else if (response.status === 409) {
        setError(data.message || "User already logged in");
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

      <button type="button" className="metal-button" onClick={handleLogin}>
        Login
      </button>
      {/* Themed feedback messages */}
      {error && <p className="auth-message error">{error}</p>}
    </div>
  );
}
