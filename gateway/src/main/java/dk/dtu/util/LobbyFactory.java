package dk.dtu.util;

/*
Author(s): Niklas
 */

import dk.dtu.model.Client;
import dk.dtu.model.Host;
import dk.dtu.model.Lobby;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public  class LobbyFactory {
    private static int nextLobbyID = 0;
    private static int nextRecreatedLobbyID = 0;

    public LobbyFactory(){}

    /**
     * @author Niklas Emil Lysdal
     * @return New Lobby Object using  ID incrementer.
     */
    public Lobby createLobby(Client creator, Host host, String lobbyName,int capacity) {
        return new Lobby(lobbyName, nextLobbyID++ + "", creator, host, capacity);
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    public Lobby recreateLobby(Client c, Host host, Map<String, String> userToPlayer, UUID saveID) {
        return new Lobby("R" + nextRecreatedLobbyID++, c, host, userToPlayer, saveID);
    }

}
