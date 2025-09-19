/*
Author(s): Lizette, Asger
*/

import React, { useState } from "react";
import { sha256Hex } from "./hashPassword";

function RegisterComp({ }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState(""); // NY state
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleRegister = async () => {
    setError("");
    setSuccess("");

    try {
      const clientHash = await sha256Hex(password);

  const response = await fetch("http://localhost:8080/api/users/create", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username,
      passwordHash: clientHash
    }),
  });


      const data = await response.json();
      console.log("Register response:", data);

      if (response.ok && data.status === "successful") {
      setSuccess("Registration successful! Please log in. ");   
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

export default RegisterComp;