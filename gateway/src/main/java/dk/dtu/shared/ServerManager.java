package dk.dtu.shared;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.dto.ClientUpdateReason;
import dk.dtu.dto.LobbyUpdateReason;
import dk.dtu.dto.UserNameUpdateEvent;
import dk.dtu.interfaces.UserDatabase;
import dk.dtu.model.*;
import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.observer.ClientObserver;
import dk.dtu.observer.LobbyObserver;
import dk.dtu.util.JsonUtil;
import dk.dtu.util.LobbyFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;

import java.util.Map;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */
@Component
public class ServerManager implements LobbyObserver, ClientObserver {
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, String> lobbyIDFromSaveID = new ConcurrentHashMap<>();
    private final Map<String, Lobby> loadedLobbies = new ConcurrentHashMap<>();
    private final Map<String, Lobby> demoLobbies = new ConcurrentHashMap<>();
    private final Map<String, String> gameToLobby = new ConcurrentHashMap<>();
    private Host host;
    private final LobbyFactory lobbyFactory;
    private final UserDatabase userDatabase;
    private final AuthManager authManager;
    private final SessionManager sessionManager;
    private final GameService gameService;
    /**
     * @author Niklas Emil Lysdal
     */
    public ServerManager(Host host, LobbyFactory lobbyFactory, DynamicUserDatabase userDatabase,AuthManager authManager,SessionManager sessionManager, GameService gameService) {


        this.host = host;
        this.lobbyFactory = lobbyFactory;
        this.userDatabase = userDatabase;
        this.authManager = authManager;
        this.sessionManager = sessionManager;
        this.gameService = gameService;
    }

    public Host getHost() {
        return this.host;
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Override
    public void handleUpdate(LobbyUpdateReason reason, Lobby lobby) {
        switch (reason) {
            case DESTROYED:
                synchronized (gameToLobby) {
                    String lobID = lobby.getLobbyID();
                    gameToLobby.entrySet().removeIf(entry -> entry.getValue().equals(lobID));
                    lobby.removeObserver(this);

                    lobbies.remove(lobID);
                    loadedLobbies.remove(lobID);
                    demoLobbies.remove(lobID);
                    lobbyIDFromSaveID.remove(lobby.getSaveID().toString());
                    notifyClientsOfUpdates("lobbies", "updatedLobbies");
                }
                break;

            // case LOCKED: {}
            // case UNLOCKED: {}

            case GAME_ENDED:

                gameToLobby.remove(lobby.getGameID().toString());
                break;
            case GAME_STARTED:
                gameToLobby.put(lobby.getGameID().toString(), lobby.getLobbyID());
                notifyClientsOfUpdates("lobbies", "updatedLobbies");
                break;
            case GAME_UPDATE:
                if (!lobby.isDemoMode()) {
                    gameService.saveGame(this, lobby);
                }
                break;
            default:

        }

    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Override
    public void handleClientUpdate(ClientUpdateReason reason, Client client) {
        System.out.println("Servermanager client update: "+reason);
        switch (reason) {
            case DISCONNECTED,LOGOUT:
                clients.remove(client.getUserID()); client.removeObserver(this); sessionManager.logOutUser(client.getUserID());
                break;

            default: return;
        }
    }

    @Override
    public void handleClientNameUpdate(Client client) {

    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @EventListener
    public void handleUsernameUpdateEvent(UserNameUpdateEvent event) {
        String newUsername = event.getNewUsername();
        String userID = event.getUserID();
        Client c = clients.get(userID);
        if (c==null) {return;}
        c.changeUsername(newUsername);
    }
    public Lobby getLobbyFromLobbyID(String lobID) {
        Lobby lob = loadedLobbies.get(lobID);
        if (lob != null) return lob;

        lob = demoLobbies.get(lobID);
        if (lob != null) return lob;

        return lobbies.get(lobID);
    }

    /**
     * @author Niklas Emil Lysdal
     * @return Returns whether the userID has a corresponding user in database.
     */
    public boolean validateUserID(String userID) {
        return userDatabase.existsID(userID);
    }


    /**
     * @author Niklas Emil Lysdal
     */
    public ArrayList<Lobby> getLobbiesListCopy() {
        return new ArrayList<>(lobbies.values());
    }

    public Client getClient(String clientID) {
        return clients.get(clientID);
    }

    public String getLobbyIDFromGameID(String gameID) {
        return gameToLobby.get(gameID);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public Lobby getLoadedLobbyFromSaveID(String saveID) {
        String lobbyID = lobbyIDFromSaveID.get(saveID);
        if (lobbyID == null) {
            return null;
        }
        return getLobbyFromLobbyID(lobbyID);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public void putClient(Client client) {
        clients.put(client.getUserID(), client);
        client.addObserver(this);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public Client createClient(User user, WebSocketSession session) {
        Client newClient = new Client(user, session);
        newClient.addObserver(this);
        putClient(newClient);
        return newClient;
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public Client removeClient(String clientID) {
        return clients.remove(clientID);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public String removeGameMapping(String gameID) {
        return gameToLobby.remove(gameID); // returns such that caller can check if object was originally in map
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public synchronized Lobby createLobby(Client creator, Host host, String lobbyName, int capacity) {
        return createLobbyBody(creator, host, lobbyName, capacity);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public synchronized Lobby createLobby(Client creator, String lobbyName, int capacity) {
        return createLobbyBody(creator, this.host, lobbyName, capacity);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    private synchronized Lobby createLobbyBody(Client creator, Host host, String lobbyName, int capacity) {
        boolean invalidLobbyName = lobbies.values().stream().anyMatch(lobby -> lobby.getLobbyName().equals(lobbyName));
        if (invalidLobbyName) {
            throw new IllegalArgumentException("LOBBY_NAME_ALREADY_EXISTS");
        }
        Lobby lob = lobbyFactory.createLobby(creator, host, lobbyName, capacity);
        lobbies.put(lob.getLobbyID(), lob);
        lob.addObserver(this);

        notifyClientsOfUpdates("lobbies", "updatedLobbies");

        return lob;
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Benjamin Benyo Endahl Hansen
     */
    public void notifyClientsOfUpdates(String type, String action) {
        ObjectNode msg = JsonUtil.createObjectNode();
        msg.put("type", type);
        msg.put("action", action);
        broadcastToClients(msg);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    private void broadcastToClients(ObjectNode msg) {
        for (Client client : clients.values()) {
                client.handleMessage(msg);
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public Lobby recreateLobby(Client c, String lobbyName, Map<String, String> userToPlayer, UUID saveID) {
        Lobby lob = lobbyFactory.recreateLobby(c, lobbyName, this.host, userToPlayer, saveID);
        lobbyIDFromSaveID.put(lob.getSaveID().toString(), lob.getLobbyID());
        loadedLobbies.put(lob.getLobbyID(), lob);
        lob.addObserver(this);
        return lob;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    public Lobby createDemoLobby(Client c) {
        Lobby lob = lobbyFactory.createDemoLobby(c, this.host);
        demoLobbies.put(lob.getLobbyID(), lob);
        lob.addObserver(this);
        return lob;
    }

    /**
     * @author Benjamin Benyo Endahl Hansen
     */
    public String getUsernameFromUUID(String UUID){
        return userDatabase.findUserById(UUID).getName();
    }


    /**
     * @author Niklas Emil Lysdal
     */

    public void setHostSession(WebSocketSession sess) {
        this.host.setSession(sess);
    }


    public boolean isConnected(String userID) {
        Client client = clients.get(userID);
        return !(client == null || !client.isSessionOpen());

    }
    public boolean existsClient(String userID) {
        return clients.containsKey(userID);
    }
    /*
     * The following functions are for test purposes only
     */

    public Map<String, Client> getClientsForTest() {
        return clients;
    }

    public Map<String, Lobby> getLobbiesForTest() {
        return lobbies;
    }

    public Map<String, Lobby> getLoadedLobbiesForTest() {
        return loadedLobbies;
    }

    public Map<String, Lobby> getDemoLobbiesForTest() {
        return demoLobbies;
    }

    public Map<String, String> getGameToLobbyForTest() {
        return gameToLobby;
    }
}
