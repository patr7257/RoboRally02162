/*
Author(s): Asger, Bjarke, Patrick
*/
let socket;
const listeners = new Set();

export function getSocket() {
  if (!socket) {
    socket = new WebSocket("ws://localhost:8080/client?token=User"); // remove 'const'

    socket.onopen = () => {
      console.log("Connected!");
    };

    socket.onmessage = (event) => {
      console.log("Message:", event.data);
    };
  }
  return socket;
}


export function subscribe(cb) {
  listeners.add(cb);
  return () => listeners.delete(cb);
}