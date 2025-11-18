package dk.dtu.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.dto.LobbyPublicJson;
import dk.dtu.dto.LobbyPrivateJson;
import dk.dtu.observer.LobbyObserver;
import dk.dtu.util.JsonUtil;
import dk.dtu.dto.OperationResult;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Niklas Emil Lysdal
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 * @author Asger Allin Jensen
 * @author Bjarke Søderhamn Petersen
 * @author Kajsa Alice Ulrika Berlstedt
 */

public class Lobby {
    private final String lobbyID;
    private String lobbyName;
    private final Map<String, Client> players = new HashMap<>();
    private final Host host;
    private final HashSet<LobbyObserver> observers = new HashSet<>();
    private boolean locked;
    //TODO: might need to store the creator of the lobby. As playerID can no longer be used to determine this.
    private boolean isRunning = false;
    private UUID gameID;
    private final UUID saveID;
    private final Map<String, String> userToPlayer;
    private final Map<String, String> playerToUser = new HashMap<>();
    private final Map<String, Boolean> playerReadinessMap = new HashMap<>();
    private final Map<String, Boolean> userNameReadinessMap = new HashMap<>();
    private int nextPlayerID = 1;
    private int capacity = 6; //TODO: get this number from either host or client
    public final boolean loadedLobby;
    private String boardTemplateName = "Random"; // Default to Random

    ExecutorService broadcastPool = Executors.newCachedThreadPool();

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */

    public Lobby(String lobbyName, String lobbyID, Client creator, Host host,int capacity) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(host, "host must not be null");
        this.lobbyID = lobbyID;
        this.lobbyName = lobbyName;
        this.host = host;
        this.capacity=capacity;
        this.userToPlayer = new HashMap<>();
        this.saveID = UUID.randomUUID();
        loadedLobby = false;
        addPlayer(creator);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public Lobby(String lobbyID, Client c, Host host, Map<String, String> userToPlayer, UUID saveID) {
        this.userToPlayer = userToPlayer;
        this.lobbyID = lobbyID;
        this.host = host;
        this.saveID = saveID;
        loadedLobby = true;
        this.capacity=userToPlayer.size();
        addPlayer(c);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Asger Allin Jensen
     */

    public synchronized OperationResult addPlayer(Client client) {
        if (locked) {
            return new OperationResult("lobby_locked");
        }
        if( players.size()>=capacity) {
            return new OperationResult("lobby_full");
        }
        players.put(client.getUserID(), client);
        playerReadinessMap.put(client.getUserID(), false);
        userNameReadinessMap.put(client.getUsername(), false);
        notifyClients();
        return new OperationResult("success");
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */

    public OperationResult removeClientByUID(String uid) {
        Client removed = players.remove(uid);
        if (removed == null) {
            return new OperationResult("user_not_in_lobby");
        } else {
            playerReadinessMap.remove(uid);
            userNameReadinessMap.remove(removed.getUsername());

            if (players.isEmpty()) {
                if (gameID != null) {
                    host.endGame(gameID);
                }
                notifyObservers(LobbyUpdateReason.DESTROYED);
                return new OperationResult("lobby_empty");
            }

            String playerID = userToPlayer.get(uid);
            playerToUser.remove(playerID);
            if (!loadedLobby) userToPlayer.remove(uid);
            notifyClients();

            return new OperationResult("success");
        }
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     * @author Patrick Røbel
     */

    public void startGame(JsonNode gameInfo) throws Exception {
        if (!areAllPlayersReady()) {
            broadcastNotReadyMessage();
            throw new Exception("Game tried to start before all players are ready");
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
        JsonNode gameSnapshot = host.saveGame(gameID);

        notifyObservers(LobbyUpdateReason.DESTROYED);

        return gameSnapshot;
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */

    public void handleClientMessage(String userID, JsonNode json) {
        ObjectNode root = JsonUtil.createObjectNode();
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

        switch (json.get("delivery").asText()) {
            case "DIRECT":
                String playerID = json.get("meta").get("player").get("playerID").asText();

                String userID = playerToUser.get(playerID);
                if (userID == null) {
                    return;
                } // in case player has disconnected
                Client client = players.get(userID);
                client.handleMessage(root);
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
        if (allPlayersHaveJoined()) {
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

    public LobbyPublicJson asPublicJson() {
       return new LobbyPublicJson(this.lobbyName,this.lobbyID,capacity,players.size(),this.isRunning);
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
        playerReadinessMap.put(uid, true);
        userNameReadinessMap.put(client.getUsername(), true);
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
        playerReadinessMap.put(uid, false);
        userNameReadinessMap.put(client.getUsername(), false);
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

}
