import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import "./lobby.css";
import "../styles/lobbyCreator.css";
import { closeSocket } from "../utils/ws";

/**
 * @author Weihao Mo
 * @author Karl Johannes Agerbo
 */
export default function UserSettings() {
  const navigate = useNavigate();
  const [newUsername, setNewUsername] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

  const currentUsername = sessionStorage.getItem("username");


  const handleChangeUsername = async () => {
    setError("");
    setSuccess("");

    if (!newUsername.trim()) {
      setError("Username cannot be empty");
      return;
    }

    if (newUsername === currentUsername) {
      setError("New username is the same as current username");
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/users/changeUsername?newUsername=${encodeURIComponent(newUsername)}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sessionStorage.getItem("userToken")}`,
          },
        }
      );

      const data = await response.text();

      if (response.ok) {
        sessionStorage.setItem("username", newUsername);
        setSuccess("Username changed successfully!");
        setNewUsername("");
      } else if (response.status === 409) {
        setError("Username already exists. Choose another.");
      } else if (response.status === 401) {
        setError("User not found. Please log in again.");
      } else {
        setError("Failed to change username. Try again.");
      }
    } catch (err) {
      console.error("Change username error:", err);
      setError("Network error. Try again.");
    }
  };

  const handleDeleteUser = async () => {
    setError("");
    setSuccess("");


    try {
      const response = await fetch(
        `${API_BASE_URL}/api/users/delete`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sessionStorage.getItem("userToken")}`,
          },
        }
      );

      const data = await response.text();

      if (response.ok) {
        sessionStorage.clear();
        navigate("/")
        closeSocket(1000);
      } else {
        setError("Failed to remove user. Try again.");
      }
    } catch (err) {
      console.error("Delete user error:", err);
      setError("Network error. Try again.");
    }
  };


  return (
    <Layout>
      <div className="panel-container">
        <h1 className="panel-title">User Settings</h1>

        <div className="lobby-settings-panel">
          <div className="lobby-id-display">
            <span className="lobby-name-label">CURRENT USERNAME</span>
            <span className="lobby-name-value">{currentUsername || "Unknown"}</span>
          </div>

          <div className="lobby-settings">
            <div className="form">
              <div className="username-form-container">
                <label htmlFor="newUsername" className="username-label">
                  New Username
                </label>
                <input
                  id="newUsername"
                  type="text"
                  className="username-input"
                  placeholder="Enter new username"
                  value={newUsername}
                  maxLength={20}
                  onChange={(e) => setNewUsername(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleChangeUsername()}
                />
              </div>
            </div>
          </div>

          <div className="settings-buttons">
            <button
              className="metal-button cancel-button"
              onClick={() => navigate("/lobbyScene")}
            >
              Back
            </button>

            <button className="metal-button" onClick={handleChangeUsername}>
              Change Username
            </button>
            <button className="metal-button" onClick={handleDeleteUser}> 
              Delete My User
            </button>
          </div>

          {error && <div className="error-text"><label>{error}</label></div>}
          {success && <p className="auth-message success">{success}</p>}
        </div>
      </div>
    </Layout>
  );
}