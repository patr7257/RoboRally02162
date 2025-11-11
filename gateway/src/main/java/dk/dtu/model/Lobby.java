package dk.dtu.model;

/*
Author(s): Niklas, Karl, Benjamin
@author Niklas Emil Lysdal
@author Karl Agerbo
@author Benjamin Benyo
@author Asger Allin Jensen
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.dto.LobbyJson;
import dk.dtu.observer.LobbyObserver;
import dk.dtu.util.JsonUtil;
import dk.dtu.dto.OperationResult;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Lobby {
    private final String lobbyID;
    private final Map<String, Client> players = new HashMap<>();
    private final Host host;
    private final HashSet<LobbyObserver> observers = new HashSet<>();
    private boolean locked;
    // TODO: might need to store the creator of the lobby. As playerID can no longer
    // be used to determine this.

    private UUID gameID;
    private final Map<String, String> userToPlayer = new HashMap<>();
    private final Map<String, String> playerToUser = new HashMap<>();
    private final Map<String, Boolean> playerReadinessMap = new HashMap<>();
    private final Map<String, Boolean> userNameReadinessMap = new HashMap<>();
    private int nextPlayerID = 1;

    ExecutorService broadcastPool = Executors.newCachedThreadPool();

    public Lobby(String lobbyID, Client creator, Host host) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(host, "host must not be null");
        this.lobbyID = lobbyID;
        this.host = host;
        addPlayer(creator);
    }

    public OperationResult addPlayer(Client client) {
        if (locked) {
            return new OperationResult("lobby_locked");
        } else {
            players.put(client.getUserID(), client);
            playerReadinessMap.put(client.getUserID(), false);
            userNameReadinessMap.put(client.getUsername(), false);
            handleUserReadyState();
            return new OperationResult("success");
        }
    }

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
            userToPlayer.remove(uid);
            handleUserReadyState();
            return new OperationResult("success");
        }
    }

    public void startGame() {
        if (!areAllPlayersReady()) {
            broadcastNotReadyMessage();
            return;
        }

        initPlayerUserMaps();
        this.locked = true; // lock before starting

        try {
            this.gameID = host.startGame(players.size(), 10); // TODO: Change the boardsize to be decided by the client
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

    public void handleClientMessage(String userID, JsonNode json) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("gameID", this.gameID.toString());
        root.put("playerID", Integer.parseInt(userToPlayer.get(userID)));
        root.set("payload", json.get("payload"));
        host.handleMessage(root);
    }

    public void handleHostMessage(JsonNode json) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", json.get("type"));
        root.set("payload", json.get("payload"));

        switch (json.get("delivery").asText()) {
            case "DIRECT":
                String playerID = json.get("meta").get("player").get("playerID").asText();

                String userID = playerToUser.get(playerID); // TODO: change to UUID
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

    private void initPlayerUserMaps() {
        for (Client client : players.values()) {
            userToPlayer.put(client.getUserID(), nextPlayerID + "");
            playerToUser.put(nextPlayerID + "", client.getUserID());
            nextPlayerID++;
        }
    }

    private void broadcastToClients(ObjectNode msg) {
        for (Client c : players.values()) {
            broadcastPool.submit(() -> c.handleMessage(msg));
        }
    }

    public UUID getGameID() {
        return gameID;
    }

    public String getLobbyID() {
        return lobbyID;
    }

    public Map<String, Client> getPlayers() {
        return players;
    }

    public Map<String, String> getUserToPlayer() {
        return userToPlayer;
    }

    public void setGameID(UUID gameID) {
        this.gameID = gameID;
    }

    public boolean isLocked() {
        return locked;
    }

    public List<String> getPlayerIDs() throws Exception {
        if (!locked) {
            throw new Exception("GAME_NOT_STARTED");
        }
        return new ArrayList<>(userToPlayer.values());
    }

    public boolean isOccupied() {
        return players.size() >= 6; // TODO: We need to ask the host for this number
    }

    public void addObserver(LobbyObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(LobbyObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(LobbyUpdateReason reason) {
        for (LobbyObserver observer : observers) {
            observer.handleUpdate(reason, this);
        }
    }

    public LobbyJson asJson() {
        return new LobbyJson(this.lobbyID); // TODO: construct lobby json.
    }

    // @author Asger Allin Jensen
    // @author Niklas Emil Lysdal
    public OperationResult playerMarkedAsReady(String uid) {
        Client client = players.get(uid);
        if (client == null) {
            return new OperationResult("user_not_in_lobby");
        }
        playerReadinessMap.put(uid, true);
        userNameReadinessMap.put(client.getUsername(), true);
        handleUserReadyState();
        return new OperationResult("success");
    }

    // @author Asger Allin Jensen
    // @author Niklas Emil Lysdal
    public OperationResult playerMarkedAsNotReady(String uid) {
        Client client = players.get(uid);
        if (client == null) {
            return new OperationResult("user_not_in_lobby");
        }
        playerReadinessMap.put(uid, false);
        userNameReadinessMap.put(client.getUsername(), false);
        handleUserReadyState();
        return new OperationResult("success");
    }

    // @author Asger Allin Jensen
    // @author Niklas Emil Lysdal
    private OperationResult handleUserReadyState() {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("lobbyID", lobbyID);
        root.put("action", "Readiness");
        ObjectNode payloadNode = JsonUtil.createObjectNode();
        for (Map.Entry<String, Boolean> entry : userNameReadinessMap.entrySet()) {
            payloadNode.put(entry.getKey(), entry.getValue());
        }
        root.set("payload", payloadNode);
        broadcastToClients(root);

        return new OperationResult("success");
    }

    // @author Asger Allin Jensen
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

    // @author Asger Allin Jensen
    private void broadcastNotReadyMessage() {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "lobby");
        root.put("action", "start_denied");
        ObjectNode payload = JsonUtil.createObjectNode();
        payload.put("reason", "Not all players are ready");
        root.set("payload", payload);

        broadcastToClients(root);
    }

    //@author Asger Allin Jensen & Kajsa Alice Ulrika Berlstedt
    public String getUserIDS () {
        return userToPlayer.keySet().toString();
    }
    public Map<String, String> getUsernamePlayerIDMaps() {
        Map<String, String> result = new HashMap<>();
        for (Client c : players.values()) {
            result.put(c.getUsername(),userToPlayer.get(c.getUserID()));

        }
        return result;
    }

}
