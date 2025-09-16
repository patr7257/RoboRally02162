package dk.dtu;

/*
Author(s): Niklas
 */

import java.util.UUID;

public class LobbyFactory {
    private int nextLobbyID = 0;


    public LobbyFactory(){}


    public Lobby createLobby(Client creator, Host host) {
        return  new Lobby(nextLobbyID++ + "" , creator, host);

    }
}
