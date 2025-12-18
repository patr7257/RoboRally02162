package dk.dtu.util;


import dk.dtu.model.Client;
import dk.dtu.model.Host;
import dk.dtu.model.Lobby;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * @author Niklas Emil Lysdal
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */

@Component
public  class LobbyFactory {
    private static int nextLobbyID = 0;
    private static int nextRecreatedLobbyID = 0;
    private static int nextDemoLobbyID = 0;

    public LobbyFactory(){}

    /**
     * @author Niklas Emil Lysdal
     * @return New Lobby Object using  ID incrementer.
     */
    public Lobby createLobby(Client creator, Host host, String lobbyName, int capacity) {
        return new Lobby(lobbyName, nextLobbyID++ + "", creator, host, capacity, false);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public Lobby recreateLobby(Client c, String lobbyName, Host host, Map<String, String> userToPlayer, UUID saveID) {
        return new Lobby("R" + nextRecreatedLobbyID++, lobbyName, c, host, userToPlayer, saveID);
    }

    /**
     * @author Karl Johannes Agerbo
     */
    public Lobby createDemoLobby(Client c, Host host) {
        return new Lobby("", "D" + nextDemoLobbyID++, c, host, 1, true);
    }

    public void resetLobbyCounters() {
        nextLobbyID = 0;
        nextRecreatedLobbyID = 0;
        nextDemoLobbyID = 0;
    }

}
