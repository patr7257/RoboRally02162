/*
Author(s): Lizette, Asger
*/

import React, { useState } from "react";
import { sha256Hex } from "./hashPassword";

interface RegisterCompProps {}

export default function RegisterComp({}: RegisterCompProps) {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;


  const handleRegister = async () => {
    setError("");
    setSuccess("");

    try {
      const clientHash = await sha256Hex(password);

      const response = await fetch(API_BASE_URL+"/api/users/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username,
          passwordHash: clientHash,
        }),
      });

      const data = await response.json();
      console.log("Register response:", data);

      if (response.ok && data.status === "successful") {
        setSuccess("Registration successful! Please log in.");
        setUsername("");
        setPassword("");
        return;
      }

      if (response.status === 409) {
        setError("Username already exists. Choose another.");
      } else {
        setError(data?.message || "Registration failed. Try again.");
      }
    } catch (err) {
      console.error("Registration error:", err);
      setError("Network error. Try again.");
    }
  };

  return (
    <div className="register-container">
      <h2>Register</h2>

      <div>
        <input
          type="text"
          placeholder="Enter username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleRegister()}
        />
      </div>

      <div>
        <input
          type="password"
          placeholder="Enter password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleRegister()}
        />
      </div>

      <button type="button" onClick={handleRegister}>
        Register
      </button>

      {error && <p style={{ color: "red" }}>{error}</p>}
      {success && <p style={{ color: "green" }}>{success}</p>}
    </div>
  );
}
