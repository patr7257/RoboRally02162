/*
Author(s): Asger
*/

import React, { useState } from "react";

function RegisterComp({ onRegister }) {
  const [username, setUsername] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleRegister = async () => {
    setError("");
    setSuccess("");

    try {
      const response = await fetch("http://localhost:8080/api/users/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username }),
      });

      const data = await response.json();

      if (response.ok && data.status === "successful") {
        setSuccess("Registration successful! Logging you in...");
        // Automatically log in after registration
        localStorage.setItem("token", data.token);
        onRegister(data.token);
      } else if (response.status === 409) {
        setError("Username already exists. Choose another.");
      } else {
        setError("Registration failed. Try again.");
      }
    } catch (err) {
      console.error("Registration error:", err);
      setError("An error occurred. Try again.");
    }
  };

  return (
    <div className="register-container">
      <h2>Register</h2>
      <input
        type="text"
        placeholder="Enter username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
      />
      <button onClick={handleRegister}>Register</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
      {success && <p style={{ color: "green" }}>{success}</p>}
    </div>
  );
}

export default RegisterComp;