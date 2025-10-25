package dk.dtu.shared;

import dk.dtu.model.*;
import dk.dtu.observer.LobbyObserver;
import dk.dtu.util.LobbyFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
/*
Author(s): Niklas
 */
@Component
public class ServerManager implements LobbyObserver {
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, String> gameToLobby = new ConcurrentHashMap<>();
    private Host host;
    private final LobbyFactory lobbyFactory;

    public ServerManager(Host host, LobbyFactory lobbyFactory) {
        this.host = host;
        this.lobbyFactory = lobbyFactory;
    }

    public Host getHost() {
        return this.host;
    }


    @Override
    public void handleUpdate(LobbyUpdateReason reason, Lobby lobby) {
        switch (reason) {
            case DESTROYED:
                synchronized (gameToLobby) {
                    String lobID = lobby.getLobbyID();
                    gameToLobby.entrySet().removeIf(entry -> entry.getValue().equals(lobID));
                    lobby.removeObserver(this);
                    lobbies.remove(lobID);
                }
                break;

            //case LOCKED: {}
            //case UNLOCKED: {}
            case GAME_ENDED:

                    gameToLobby.remove(lobby.getGameID().toString());
                    break;
            case GAME_STARTED:
                    gameToLobby.put(lobby.getGameID().toString(), lobby.getLobbyID());
                    break;
            default:


        }

    }


    public Lobby getLobbyFromID(String lobID) {
        return lobbies.get(lobID);
    }

    public ArrayList<Lobby> getLobbiesListCopy() {
        return new ArrayList<>(lobbies.values());
    }
    public Client getClient(String clientID) {
        return clients.get(clientID);
    }
    public String getLobbyFromGameID(String gameID) {
        return gameToLobby.get(gameID);
    }
    public void putLobby(Lobby lobby) {
        lobbies.put(lobby.getLobbyID(), lobby);
    }
    public void putClient(Client client) {
        clients.put(client.getUsername(),client);
    }

    public Client createClient(User user, WebSocketSession session) {
        Client newClient = new Client(user, session);
        putClient(newClient);
        return newClient;
    }

    public Client removeClient(String clientID) {
        return clients.remove(clientID);
    }
    public String removeGameMapping(String gameID) {
        return gameToLobby.remove(gameID); //returns such that caller can check if object was originally in map
    }


    public Lobby createLobby(Client creator, Host host) {
        Lobby lob = lobbyFactory.createLobby(creator,host);
        lobbies.put(lob.getLobbyID(), lob);
        lob.addObserver(this);
        return lob;
    }

    public Lobby createLobby(Client creator) {
        Lobby lob = lobbyFactory.createLobby(creator,this.host);
        lobbies.put(lob.getLobbyID(), lob);
        lob.addObserver(this);
        return lob;
    }

    public void setHostSession(WebSocketSession sess) { this.host.setSession(sess);}

    /**
    The following functions are for test purposes only
    */
    public Map<String, Client> getClientsForTest() {
        return clients;
    }

    public Map<String, Lobby> getLobbiesForTest() {
        return lobbies;
    }

    public Map<String, String> getGameToLobbyForTest() {
        return gameToLobby;
    }
}

//put
//remove
//get
