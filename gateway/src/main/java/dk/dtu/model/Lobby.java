package dk.dtu.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.dto.*;
import dk.dtu.observer.ClientObserver;
import dk.dtu.observer.LobbyObserver;
import dk.dtu.util.JsonUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Niklas Emil Lysdal
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 * @author Asger Allin Jensen
 * @author Bjarke Søderhamn Petersen
 * @author Kajsa Alice Ulrika Berlstedt
 */

public class Lobby  implements ClientObserver {
    private final String lobbyID;
    private String lobbyName;
    private final Map<String, Client> players = new ConcurrentHashMap<>();
    private final Host host;
    private final Set<LobbyObserver> observers = ConcurrentHashMap.newKeySet();
    private boolean locked;
    //might need to store the creator of the lobby. As playerID can no longer be used to determine this.
    private boolean isRunning = false;
    private UUID gameID;
    private final UUID saveID;
    private final Map<String, String> userToPlayer;
    private final Map<String, String> playerToUser = new ConcurrentHashMap<>();
    private final Set<String> disconnectedPlayers = ConcurrentHashMap.newKeySet();
    //always use the changeReadyMapStatus and removeFromReadiness functions instead of direct access.
    private final Map<String, Boolean> playerReadinessMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userNameReadinessMap = new ConcurrentHashMap<>();
    private int nextPlayerID = 1;
    private int capacity = 6;
    public final boolean loadedLobby;
    private String boardTemplateName = "Random"; // Default to Random
    private final boolean demoMode;

    ExecutorService broadcastPool = Executors.newCachedThreadPool();

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */

    public Lobby(String lobbyName, String lobbyID, Client creator, Host host, int capacity, boolean demoMode) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(host, "host must not be null");
        this.lobbyID = lobbyID;
        this.lobbyName = lobbyName;
        this.host = host;
        this.capacity = capacity;
        this.userToPlayer = new HashMap<>();
        this.saveID = UUID.randomUUID();
        this.demoMode = demoMode;
        loadedLobby = false;
        addPlayer(creator);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public Lobby(String lobbyID, String lobbyName, Client c, Host host, Map<String, String> userToPlayer, UUID saveID) {
        this.userToPlayer = userToPlayer;
        this.lobbyID = lobbyID;
        this.host = host;
        this.saveID = saveID;
        loadedLobby = true;
        this.capacity = userToPlayer.size();
        this.lobbyName = lobbyName;
        this.demoMode = false;
        addPlayer(c);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Asger Allin Jensen
     */

    public synchronized OperationResult addPlayer(Client client) {
        if (!disconnectedPlayers.contains(client.getUserID())) {
            if (locked) {
                return new OperationResult("lobby_locked");
            }
            if( players.size()>=capacity) {
                return new OperationResult("lobby_full");
            }
        } else {
            disconnectedPlayers.remove(client.getUserID());
        }
        players.put(client.getUserID(), client);
        client.addObserver(this);
        if (!isRunning) { //only update these maps if game is running.
            changeReadyMapStatus(client,false);
            notifyClients(); // if it is running then no other users should ever notice that connection was lost.
        }
        return new OperationResult("success");
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */

    public OperationResult removeClientByUID(String uid)  {
        Client removed = players.remove(uid);

        if (removed == null) {
            return new OperationResult("user_not_in_lobby");
        } else {
            if (isRunning) {
                disconnectedPlayers.add(removed.getUserID());
            }
            removeFromReadiness(removed);
            removed.removeObserver(this);
            if (checkEndGame()) {
                return new OperationResult("lobby_empty");
            }

            notifyClients();

            return new OperationResult("success");
        }
    }
    /**
     * @author Niklas Emil Lysdal
     * @return True if game is ended, false if not
     */
    private boolean checkEndGame() {
        if (players.isEmpty()) {
            if (gameID != null) {
                host.endGame(gameID);
            }
            notifyObservers(LobbyUpdateReason.DESTROYED);
            return true;
        }
        return false;
    }
    /**
     * @author Niklas Emil Lysdal
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     * @author Patrick Røbel
     */

    public void startGame(JsonNode gameInfo) throws Exception {
        if(isRunning) {
            return;
        }
        if (!demoMode) {
            if (!areAllPlayersReady()) {
                broadcastNotReadyMessage();
                throw new Exception("Game tried to start before all players are ready");
            }
        }

        if (loadedLobby) {
            initPlayerUserMapsLoadedLobby();
        } else {
            initPlayerUserMaps();
        }

        this.locked = true; //lock before
        try {
            if (loadedLobby) {
                this.gameID = host.startLoadedGame(players.size(), 10, gameInfo);
            } else if (demoMode) {
                this.gameID = host.startDemoGame(gameInfo);
                host.toggleDemo(gameID);
                host.setDemoTimings(gameID);
            } else {
                this.gameID = host.startGame(players.size(), 10); // TODO: Change the boardsize to be decided by the client
            }

            this.isRunning = true;
            notifyObservers(LobbyUpdateReason.GAME_STARTED);

            ObjectNode root = JsonUtil.createObjectNode();
            root.put("type", "game");

            ObjectNode payload = JsonUtil.createObjectNode();
            payload.put("action", "start");

            root.set("payload", payload);
            broadcastToClients(root);
        } catch (Exception e) {
            this.locked = false; // game failed to start, unlock it again
            System.out.println("Failed to start game: " + e.getMessage());
            // Consider broadcasting an error message to clients here
        }
    }

    /**
     * Starts a game with a board template.
     * @author Patrick Røbel
     */
    public void startGameWithTemplate(JsonNode boardTemplate) throws Exception {
        if (!areAllPlayersReady()) {
            broadcastNotReadyMessage();
            throw new Exception("Game tried to start before all players are ready");
        }

        if (loadedLobby) {
            initPlayerUserMapsLoadedLobby();
        } else {
            initPlayerUserMaps();
        }

        this.locked = true;
        try {
            this.gameID = host.startGameWithTemplate(players.size(), boardTemplate);
            this.isRunning = true;
            notifyObservers(LobbyUpdateReason.GAME_STARTED);

            ObjectNode root = JsonUtil.createObjectNode();
            root.put("type", "game");

            ObjectNode payload = JsonUtil.createObjectNode();
            payload.put("action", "start");

            root.set("payload", payload);
            broadcastToClients(root);
        } catch (Exception e) {
            this.locked = false;
            System.out.println("Failed to start game with template: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public JsonNode saveGame() {
        return host.saveGame(gameID);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */

    public void handleClientMessage(String userID, JsonNode json) {
        if (!isRunning) {
            return; //drop message
        }

        ObjectNode root = JsonUtil.createObjectNode();
        if (userToPlayer.get(userID)==null) {
            return; //drop message
        }
        root.put("gameID", this.gameID.toString());
        root.put("playerID", Integer.parseInt(userToPlayer.get(userID)));
        root.set("payload", json.get("payload"));
        host.handleMessage(root);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */

    public void handleHostMessage(JsonNode json) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", json.get("type"));
        root.set("payload", json.get("payload"));

        String type = json.get("type").asText();
        if (type.equals("programmingStarted") || type.equals("gameFinished")) {
            //System.out.println("GAME IS SAVING!");
            notifyObservers(LobbyUpdateReason.GAME_UPDATE);
        }

        switch (json.get("delivery").asText()) {
            case "DIRECT":
                String playerID = json.get("meta").get("player").get("playerID").asText();

                String userID = playerToUser.get(playerID);
                if (userID == null) {
                    return;
                }
                Client client = players.get(userID);
                if (client != null) {
                    client.handleMessage(root);
                }

                break;
            case "BROADCAST":
                broadcastToClients(root);

                break;
            default:
                break;
        }
    }

    /**
     * @author Niklas Emil Lysdal
     */

    private void initPlayerUserMaps() {
        for (Client client : players.values()) {
            userToPlayer.put(client.getUserID(), nextPlayerID + "");
            playerToUser.put(nextPlayerID + "", client.getUserID());
            nextPlayerID++;
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private void initPlayerUserMapsLoadedLobby() throws Exception {
        if (allPlayersHaveJoined() || demoMode) {
            for (Client client : players.values()) {
                playerToUser.put(userToPlayer.get(client.getUserID()), client.getUserID());
            }
        } else {
            throw new Exception("Not all players have joined!");
        }
    }

    /**
     * @author Karl Johannes Agerbo
     */

    private boolean allPlayersHaveJoined() {
        return userToPlayer.keySet().equals(players.keySet());
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */

    private void broadcastToClients(ObjectNode msg) {
        for (Client c : players.values()) {
            broadcastPool.submit(() -> c.handleMessage(msg));
        }
    }

    public UUID getGameID() {
        return gameID;
    }

    public String getLobbyName() {return this.lobbyName;}

    public String getLobbyID() {
        return lobbyID;
    }

    public String getBoardTemplateName() {
        return boardTemplateName;
    }

    public void setBoardTemplateName(String boardTemplateName) {
        this.boardTemplateName = boardTemplateName;
    }

    public UUID getSaveID() {
        return saveID;
    }

    public Map<String, Client> getPlayers() {
        return players;
    }

    public Map<String, String> getUserToPlayer() {
        return userToPlayer;
    }

    public Map<String, String> getPlayerToUser() {
        return playerToUser;
    }

    public void setGameID(UUID gameID) {
        this.gameID = gameID;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * @author Niklas Emil Lysdal
     */

    public List<String> getPlayerIDs() throws Exception {
        if (!locked) {
            throw new Exception("GAME_NOT_STARTED");
        }
        return new ArrayList<>(userToPlayer.values());
    }

    public boolean isOccupied(){
        return players.size() >= capacity;
    }

    /**
     * @author Niklas Emil Lysdal
     */

    public void addObserver(LobbyObserver observer) {
        observers.add(observer);
    }

    /**
     * @author Niklas Emil Lysdal
     */

    public void removeObserver(LobbyObserver observer) {
        observers.remove(observer);
    }

    /**
     * @author Niklas Emil Lysdal
     */

    private void notifyObservers(LobbyUpdateReason reason) {
        for (LobbyObserver observer : observers) {
            observer.handleUpdate(reason, this);
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public void notifyGameSaved(boolean succeed) {
        if (succeed) {
            ObjectNode root = JsonUtil.createObjectNode();
            root.put("type", "gameSaved");

            broadcastToClients(root);
        } else {
            ObjectNode root = JsonUtil.createObjectNode();
            root.put("type", "error");

            ObjectNode payload = JsonUtil.createObjectNode();
            payload.put("message", "Error in saving game");

            root.set("payload", payload);

            broadcastToClients(root);
        }
    }

    /**
     * @author Niklas Emil Lysdal
     */

    public LobbyPublicJson asPublicJson(String userID) {
        boolean joinable= disconnectedPlayers.contains(userID) || (!isRunning && players.size()<capacity);
       return new LobbyPublicJson(this.lobbyName,this.lobbyID,capacity,players.size(),this.isRunning,joinable);
    }

    /**
     * @author Niklas Emil Lysdal
     */

    public LobbyPrivateJson asPrivateJson() {
        return new LobbyPrivateJson(this.lobbyName,this.lobbyID,capacity,players.size(),this.isRunning,userNameReadinessMap,this.boardTemplateName);
    }

    /**
     * @author Karl Johannes Agerbo
     */

    public boolean isLoadedLobby() {
        return loadedLobby;
    }

    /**
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */

    public OperationResult playerMarkedAsReady(String uid) {
        Client client = players.get(uid);
        if (client == null) {
            return new OperationResult("user_not_in_lobby");
        }
        changeReadyMapStatus(client,true);
        notifyClients();
        return new OperationResult("success");
    }

    /**
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */

    public OperationResult playerMarkedAsNotReady(String uid) {
        Client client = players.get(uid);
        if (client == null) {
            return new OperationResult("user_not_in_lobby");
        }
        changeReadyMapStatus(client,false);

        notifyClients();
        return new OperationResult("success");
    }


    /**
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */

     //notify participants that lobby info has updated
    public OperationResult notifyClients () {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "lobby");
        root.put("lobbyID", lobbyID);
        root.put("action", "lobbyUpdate");
        broadcastToClients(root);
        return new OperationResult("success");
    }

    /**
     *
     * @author Niklas Emil Lysdal
     */
    @Override
    public void handleClientUpdate(ClientUpdateReason reason,Client client) {
        System.out.println("LOBBY client updated - ClientUpdateReason: " + reason);
        switch (reason) {
            case DISCONNECTED,LOGOUT,RESET: {
                removeClientByUID(client.getUserID());

            }
            default: {return;}
        }
    }



    /**
     * @author Niklas Emil Lysdal
     */

    public boolean hasParticipant(String uid) {
        return players.containsKey(uid);
    }

    /**
     * @author Asger Allin Jensen
     */

    private boolean areAllPlayersReady() {
        if (players.isEmpty()) {
            return false;
        }

        for (String uid : players.keySet()) {
            Boolean ready = playerReadinessMap.get(uid);
            if (ready == null || !ready) {
                return false;
            }
        }
        return true;
    }

    /**
     * @author Asger Allin Jensen
     */

    private void broadcastNotReadyMessage() {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "lobby");
        root.put("action", "start_denied");
        ObjectNode payload = JsonUtil.createObjectNode();
        payload.put("reason", "Not all players are ready");
        root.set("payload", payload);

        broadcastToClients(root);
    }

    /**
     * @author Asger Allin Jensen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    public String getUserIDS () {
        return userToPlayer.keySet().toString();
    }


    private void changeReadyMapStatus(Client c, boolean newStatus) {
        playerReadinessMap.put(c.getUserID(), newStatus);
        userNameReadinessMap.put(c.getUsername(), newStatus);
    }
    private void removeFromReadiness(Client c) {
        playerReadinessMap.remove(c.getUserID());
        userNameReadinessMap.remove(c.getUsername());
    }

    /**
     * @author Kajsa Alice Ulrika Berlstedt
     * @author Niklas Emil Lysdal
     */

    public Map<String, String> getUsernamePlayerIDMaps() {
        Map<String, String> result = new HashMap<>();
        for (Client c : players.values()) {
            result.put(c.getUsername(),userToPlayer.get(c.getUserID()));

        }
        return result;
    }


    //Testing functions:
    //since websocket messages are only forwarded for running games
    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }



    public boolean isDemoMode() {
        return demoMode;
    }

}
