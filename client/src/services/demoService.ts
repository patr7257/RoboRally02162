import { useState } from "react";

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

/** 
* @author William Pii Jæger
* @author Karl Johannes Agerbo
*/
export function useDemoService() {
  const [lobbies, setLobbies] = useState<string[]>([]);
  const [lobbyId, setLobbyId] = useState<string>();

  /** 
  * @author William Pii Jæger
  * @author Karl Johannes Agerbo
  */
  const loadAndStartDemoGame = async (demoName: string, setError?: (msg: string) => void) => {
    if (setError) setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/lobby/createAndStartDemo", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("userToken")}`,
        },
        body: JSON.stringify({ demoTemplate: demoName })
      });
      if (!response.ok) {
        const msg = await response.text();
        throw new Error(msg || "Failed to load demo games");
      }
      const data = await response.text();
      setLobbyId(data);
      return data;
    } catch (err) {
      console.error("load demo error:", err);
      if (setError) setError("Network error. Try Again.");
      return null
    }
  };

  /** 
  * @author William Pii Jæger
  * @author Karl Johannes Agerbo
  */
  const getDemos = async (setError: ((msg: string) => void) | null) => {
    if (setError) setError("");
    try {
      const response = await fetch(API_BASE_URL + "/api/demo/get", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("userToken")}`,
        },
      });
      if (!response.ok) {
        const msg = await response.text();
        throw new Error(msg || "Failed to get demo games");
      }
      const data = await response.json();
      setLobbies(data);
    } catch (err) {
      console.error("get demo error:", err);
      if (setError) setError("Network error. Try Again.");
    }
  };

  return { lobbies, lobbyId, loadAndStartDemoGame, getDemos };
}

