import React, { useState } from "react";
import { sha256Hex } from "../utils/hashPassword";

/**
 * @author Asger Allin Jensen
 * @author Lizette Bloch Dahl Nikolajsen
 */
interface RegisterCompProps { }


/**
 * @author Asger Allin Jensen
 * @author Lizette Bloch Dahl Nikolajsen
 */
export default function RegisterComp({ }: RegisterCompProps) {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;


  const handleRegister = async () => {
    setError("");
    setSuccess("");
    // Block empty username or password BEFORE hashing
    if (!username.trim() || !password.trim()) {
      setError("Username and password cannot be empty");
      return;
    }

    try {
      const clientHash = await sha256Hex(password);

      const response = await fetch(API_BASE_URL + "/api/users/create", {
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

      <button type="button" className="metal-button" onClick={handleRegister}>
        Register
      </button>
      {/* Themed feedback messages */}
      {error && <p className="auth-message error">{error}</p>}
      {success && <p className="auth-message success">{success}</p>}
    </div>
  );
}
