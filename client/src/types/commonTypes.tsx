/*
Author(s): Asger
*/
export interface PlayerInfo {
    id: string;
    name: string;
}

export interface LobbyInfo {
    lobbyId: string;
    host: string;
    players: PlayerInfo[];
}