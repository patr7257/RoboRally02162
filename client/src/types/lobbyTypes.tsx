/**
 * @author Niklas Emil Lysdal
 */

export interface lobbyInfo {
    lobbyID: string;
    lobbyName: string;
    playerCount: number;
    capacity: number;
    status: boolean; //is it running or not.
}

export const DEFAULT_LOBBY_INFO: lobbyInfo = {
    lobbyID: "", // Assuming empty string means inactive
    capacity: 0,
    playerCount: 0,
    lobbyName: "",
    status: false,
};

export interface fullLobbyInfo {
    lobbyID: string;
    lobbyName: string;
    playerCount: number;
    capacity: number;
    status: boolean; //is it running or not.
    readinessMap: Record<string,boolean>;
}

export const DEFAULT_FULL_LOBBY_INFO: fullLobbyInfo = {
    lobbyID: "", // Assuming empty string means inactive
    capacity: 0,
    playerCount: 0,
    lobbyName: "",
    status: false,
    readinessMap: {}
};
