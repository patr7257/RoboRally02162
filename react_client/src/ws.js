/*
Author(s): Asger, Bjarke, Patrick
*/

let socket = null;
let listeners = new Set();
let messageQueue = [];
let isProcessing = false;

export function getSocket() {
  const usernameInput = localStorage.getItem("username");
  if (!socket || socket.readyState === WebSocket.CLOSED) {
    socket = new WebSocket("ws://localhost:8080/client?token=" + usernameInput);

    socket.onopen = () => {
      console.log("Connected!");
      processQueue();
    };
    
    socket.onmessage = (event) => {
      listeners.forEach((cb) => cb(event.data));
    };
    
    socket.onclose = (e) => {
      console.log("WebSocket closed", {
        code: e.code, 
        reason: e.reason, 
        wasClean: e.wasClean
      });
      socket = null;
    };
  }
  return socket;
}

export function sendMessage(data) {
  const message = typeof data === 'string' ? data : JSON.stringify(data);
  
  messageQueue.push(message);
  processQueue();
  
  return true;
}

function processQueue() {
  if (isProcessing || messageQueue.length === 0) {
    return;
  }
  
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    return;
  }
  
  isProcessing = true;
  
  const message = messageQueue.shift();
  
  try {
    socket.send(message);
    console.log("Sent message:", message);
  } catch (error) {
    console.error("Error sending message:", error);
    messageQueue.unshift(message);
  }
  
  isProcessing = false;
  
  if (messageQueue.length > 0) {
    setTimeout(processQueue, 50);
  }
}

export function subscribe(cb) {
  listeners.add(cb);
  getSocket(); 
  return () => listeners.delete(cb);
}

export function closeSocket() {
  if (socket) socket.close();
  socket = null;
  messageQueue = [];
}

export function getQueueSize() {
  return messageQueue.length;
}