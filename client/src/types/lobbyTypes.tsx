/**
 * @author Niklas Emil Lysdal
 */

export interface lobbyInfo {
    lobbyID: string;
    lobbyName: string;
    playerCount: number;
    capacity: number;
    isRunning: boolean; //is it running or not.
    canJoin: boolean
}

export const DEFAULT_LOBBY_INFO: lobbyInfo = {
    lobbyID: "", // Assuming empty string means inactive
    capacity: 0,
    playerCount: 0,
    lobbyName: "",
    isRunning: false,
    canJoin: false,
};

export interface fullLobbyInfo {
    lobbyID: string;
    lobbyName: string;
    playerCount: number;
    capacity: number;
    isRunning: boolean; //is it running or not.
    readinessMap: Record<string, boolean>;
}

export const DEFAULT_FULL_LOBBY_INFO: fullLobbyInfo = {
    lobbyID: "", // Assuming empty string means inactive
    capacity: 0,
    playerCount: 0,
    lobbyName: "",
    isRunning: false,
    readinessMap: {}
};
