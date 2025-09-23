package dk.dtu.util;

/*
Author(s): Niklas
 */

import dk.dtu.model.Client;
import dk.dtu.model.Host;
import dk.dtu.model.Lobby;

public  class LobbyFactory {
    private static int nextLobbyID = 0;


    public LobbyFactory(){}


    public static Lobby createLobby(Client creator, Host host) {
        return  new Lobby(nextLobbyID++ + "" , creator, host);

    }
}
