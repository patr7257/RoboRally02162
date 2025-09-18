/*
Author(s): Asger
*/

import React, { useState } from "react";
import { getSocket, subscribe} from "./ws";

function LoginComp({ onLogin }) {
  const [usernameInput, setUsernameInput] = useState("");
  const [error, setError] = useState("");

  const handleLogin = async () => {
    

    setError("");
    try {
      const response = await fetch("http://localhost:8080/api/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: usernameInput }),
      });

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem("token", data.token);
        localStorage.setItem("username", usernameInput);
        console.log("pass: "+usernameInput)
        await getSocket();
        subscribe((message) => {
          console.log("Received game message:", message);
        });

        onLogin(data.token); // update Home state
      } else if (response.status === 401) {
        setError("Login failed. User does not exist.");
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
      <input
        type="text"
        placeholder="Enter username"
        value={usernameInput}
        onChange={(e) => setUsernameInput(e.target.value)}
      />
      <button onClick={handleLogin}>Login</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
    </div>
  );
}

export default LoginComp;