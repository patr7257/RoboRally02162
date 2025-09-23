package dk.dtu.shared;

import dk.dtu.model.Client;
import dk.dtu.model.Host;
import dk.dtu.model.Lobby;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/*
Author(s): Niklas
 */
@Component
public class ServerRegistry {
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, String> gameToLobby = new ConcurrentHashMap<>();
    private Host host;
    public ServerRegistry(Host host) {
        this.host = host;
    }
    public Map<String, Client> getClients() {
        return clients;
    }

    public Map<String, Lobby> getLobbies() {
        return lobbies;
    }

    public Map<String, String> getGameToLobby() {
        return gameToLobby;
    }
    public Host getHost() {return this.host;}
}
