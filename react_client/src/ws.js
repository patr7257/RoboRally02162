/*
Author(s): Asger, Bjarke, Patrick
*/
let socket = null;
let listeners = new Set();
let unsubscribe = null;

export function getSocket() {
  const usernameInput = localStorage.getItem("username");
  if (!socket || socket.readyState === WebSocket.CLOSED) {
    socket = new WebSocket("ws://localhost:8080/client?token=" + usernameInput);

    socket.onopen = () => console.log("Connected!");
    socket.onmessage = (event) => {
      listeners.forEach((cb) => cb(event.data));
    };
    socket.onclose = () => {
      console.log("WebSocket closed");
      socket = null;
      unsubscribe = null;
    };
  }
  return socket;
}

export function subscribe(cb) {
  listeners.add(cb);
  unsubscribe = () => listeners.delete(cb);
  return unsubscribe;
}

export function closeSocket() {
  if (unsubscribe) unsubscribe();
  if (socket) socket.close();
  socket = null;
  unsubscribe = null;
}