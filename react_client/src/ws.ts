/*
Author(s): Asger, Bjarke, Patrick
*/

let socket: WebSocket | null = null;
let listeners: Set<(message: string) => void> = new Set();
let messageQueue: string[] = [];
let isProcessing = false;

export function getSocket(): WebSocket {
  const usernameInput: string | null = localStorage.getItem("username");
  if (!socket || socket.readyState === WebSocket.CLOSED) {
    socket = new WebSocket("ws://localhost:8080/client?token=" + usernameInput);

    socket.onopen = () => {
      console.log("Connected!");
      processQueue();
    };

    socket.onmessage = (event: MessageEvent) => {
      listeners.forEach((cb) => cb(event.data));
    };

    socket.onclose = (e: CloseEvent) => {
      console.log("WebSocket closed", {
        code: e.code,
        reason: e.reason,
        wasClean: e.wasClean,
      });
      socket = null;
    };
  }
  return socket;
}

export function sendMessage(data: string | object): boolean {
  const message = typeof data === "string" ? data : JSON.stringify(data);

  messageQueue.push(message);
  processQueue();

  return true;
}

function processQueue(): void {
  if (isProcessing || messageQueue.length === 0) {
    return;
  }

  if (!socket || socket.readyState !== WebSocket.OPEN) {
    return;
  }

  isProcessing = true;

  const message = messageQueue.shift()!;

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

export function subscribe(cb: (message: string) => void): () => void {
  listeners.add(cb);
  getSocket();
  return () => listeners.delete(cb);
}

export function closeSocket(): void {
  if (socket) socket.close();
  socket = null;
  messageQueue = [];
}

export function getQueueSize(): number {
  return messageQueue.length;
}
