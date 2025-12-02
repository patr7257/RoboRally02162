import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
let socket: WebSocket | null = null;
let listeners: Set<(message: string) => void> = new Set();
let messageQueue: string[] = [];
let isProcessing = false;
let reconnectInterval = 3000; //3000 for production, 11000 for testing logout navigation
let reconnectAttempts = 0;
const maxReconnectAttempts = 10;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
function getWsUrl(reason: string): string {
    const userID: string | null = sessionStorage.getItem("userID");
    const token = sessionStorage.getItem("userToken");
    const wsProtocol = window.location.protocol === "https:" ? "wss" : "ws";
    const wsHost = process.env.REACT_APP_API_WS_URL!.replace(/^ws(s)?:\/\//, ''); // remove ws:// prefix
    return `${wsProtocol}://${wsHost}/client?token=${token}&reason=${reason}`;
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export function getSocket(reason: string): WebSocket |null {
    if (!sessionStorage.getItem("userToken")) { //not logged in shouldn't have a socket
        if (socket) {
            socket.close();
            socket = null;
        }
        return null;
    }
    


    if (socket && socket.readyState === WebSocket.OPEN) {
        return socket;
    }

    if (!socket || socket.readyState === WebSocket.CLOSED) {
        const wsUrl = getWsUrl(reason);
        socket = new WebSocket(wsUrl);
        // to allow manual closing in console
        // @ts-ignore
        window.debugSocket = socket;
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
            if (e.code == 4001) {
                //console.log("websocket connection failed due to invalid user state"); since the navigate clears the state  the console is also cleared
                sessionStorage.clear();
                sessionStorage.setItem("returnReason", "Error with connection. You have been logged out.");

                window.location.href = "/"; //navigate but can't use react hook. also cleans memory
                return;
            }
            if (e.code !== 1000) {


                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++;
                    console.log(`Reconnecting in ${reconnectInterval / 1000}s...`);
                    setTimeout(() => getSocket("RECONNECT"), reconnectInterval);
                }
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
    getSocket("RECONNECT"); //reconnect if connection closed.
    return () => listeners.delete(cb);
}

/**
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 */
export function closeSocket(reason: number): void {
    if (socket) socket.close(reason);
    socket = null;
    messageQueue = [];
    listeners.clear();
}

export function getQueueSize(): number {
    return messageQueue.length;
}

/**
 * @author Niklas Emil Lysdal
 * to allow websocket to reconnect even on pages that don't subscribe to it
 */
export const WebSocketManager = () => {
    const location = useLocation();
    useEffect(()=> {
        getSocket("RECONNECT");
    },[Location])
    return null;
}