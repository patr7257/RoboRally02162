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

    public Lobby createLobby(Client creator, Host host) {
        return new Lobby(nextLobbyID++ + "" , creator, host);
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
