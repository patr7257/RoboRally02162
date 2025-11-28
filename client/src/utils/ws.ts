let socket: WebSocket | null = null;
let listeners: Set<(message: string) => void> = new Set();
let messageQueue: string[] = [];
let isProcessing = false;
let reconnectInterval = 3000;
let reconnectAttempts = 0;
const maxReconnectAttempts = 10;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
function getWsUrl(): string {
  const userID: string | null = localStorage.getItem("userID");
  const token = localStorage.getItem("userToken");
  const wsProtocol = window.location.protocol === "https:" ? "wss" : "ws";
  const wsHost = process.env.REACT_APP_API_WS_URL!.replace(/^ws(s)?:\/\//, ''); // remove ws:// prefix
  return `${wsProtocol}://${wsHost}/client?token=${token}`;
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export function getSocket(): WebSocket {
  if (socket && socket.readyState === WebSocket.OPEN) {
    return socket;
  }

  if (!socket || socket.readyState === WebSocket.CLOSED) {
    const wsUrl = getWsUrl();
    socket = new WebSocket(wsUrl);

    socket.onopen = () => {
      console.log("WebSocket connected!");
      reconnectAttempts = 0;
      processQueue();
    };

    socket.onmessage = (event: MessageEvent) => {
      listeners.forEach((cb) => cb(event.data));
    };

    socket.onclose = (e: CloseEvent) => {
      console.warn("WebSocket closed", {
        code: e.code,
        reason: e.reason,
        wasClean: e.wasClean,
      });
      socket = null;

      if (reconnectAttempts < maxReconnectAttempts && !e.wasClean) {
        reconnectAttempts++;
        console.log(`Reconnecting in ${reconnectInterval / 1000}s...`);
        setTimeout(getSocket, reconnectInterval);
      }
    };

    socket.onerror = (err) => {
      console.error("WebSocket error:", err);
      // Socket will close automatically and trigger onclose
    };
  }

  return socket;
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export function sendMessage(data: string | object): boolean {
  const message = typeof data === "string" ? data : JSON.stringify(data);
  messageQueue.push(message);
  processQueue();
  return true;
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
function processQueue(): void {
  if (isProcessing || messageQueue.length === 0) return;
  if (!socket || socket.readyState !== WebSocket.OPEN) return;

  isProcessing = true;
  const message = messageQueue.shift()!;

  try {
    socket.send(message);
    console.log("Sent message:", message);
  } catch (error) {
    console.error("Error sending message, re-queueing:", error);
    messageQueue.unshift(message);
  }

  isProcessing = false;

  if (messageQueue.length > 0) {
    setTimeout(processQueue, 50);
  }
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export function subscribe(cb: (message: string) => void): () => void {
  listeners.add(cb);
  getSocket();
  return () => listeners.delete(cb);
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export function closeSocket(): void {
  if (socket) socket.close();
  socket = null;
  messageQueue = [];
  listeners.clear();
}

export function getQueueSize(): number {
  return messageQueue.length;
}
