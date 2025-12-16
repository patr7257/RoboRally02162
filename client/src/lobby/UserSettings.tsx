import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "./Layout";
import "../styles/lobby.css";
import "../styles/lobbyCreator.css";
import "../styles/joinLobby.css";
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
  const [showUsernamePopup, setShowUsernamePopup] = useState<boolean>(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState<boolean>(false);
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
        setShowUsernamePopup(false);
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
        sessionStorage.setItem("returnReason", "Your account has been deleted. \nRegister a new profile to continue.");
        sessionStorage.removeItem("userToken");
        sessionStorage.removeItem("username");
        sessionStorage.removeItem("userID");
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
      <div className="page-title" style={{ marginTop: '30px' }}>
        <h1 className="metal-text">User Settings</h1>
      </div>
      <div className="control-panel">
        <div className="lobby-id-display">
          <span className="lobby-name-label">CURRENT USERNAME</span>
          <span className="lobby-name-value">{currentUsername || "Unknown"}</span>
        </div>

        <div className="settings-buttons-column">
          <button className="metal-button small" onClick={() => setShowUsernamePopup(true)}>
            Change Username
          </button>
          <button className="metal-button small" onClick={() => setShowDeleteConfirmation(true)}>
            Delete My User
          </button>
          <button className="metal-button icon" onClick={() => navigate("/lobbyScene")}>
            <div className="exit-icon"></div>
          </button>
        </div>
      </div>

      {success && (
        <div className="success-overlay" onClick={() => setSuccess("")}>
          <div className="success-popup" onClick={(e) => e.stopPropagation()}>
            <p className="success-message">{success}</p>
          </div>
        </div>
      )}

      {showUsernamePopup && (
        <div className="modal-overlay" onClick={() => setShowUsernamePopup(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px', width: '90%' }}>
            <h2 className="modal-title">Change Username</h2>
            <div className="username-form-container">
              <label htmlFor="newUsername" className="username-label">
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
            {error && <div className="error-text"><label>{error}</label></div>}
            <div className="modal-buttons" style={{ flexDirection: 'column', gap: '10px', alignItems: 'center' }}>
              <button className="metal-button small" onClick={handleChangeUsername}>
                Confirm
              </button>
              <button className="metal-button small" onClick={() => {
                setShowUsernamePopup(false);
                setError("");
              }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeleteConfirmation && (
        <div className="confirmation-overlay">
          <div className="confirmation-modal">
            <div className="confirmation-content">
              <p className="confirmation-text">
                ARE YOU SURE YOU WANT TO DELETE YOUR USER?
                <br />
                THIS ACTION CANNOT BE UNDONE.
              </p>
            </div>
            <div className="confirmation-buttons">
              <button
                className="metal-button icon"
                onClick={handleDeleteUser}
                aria-label="Yes, delete user"
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>YES</span>
              </button>
              <button
                className="metal-button icon"
                onClick={() => setShowDeleteConfirmation(false)}
                aria-label="No, cancel"
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>NO</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}