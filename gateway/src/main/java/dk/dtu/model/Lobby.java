package dk.dtu.model;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;
import dk.dtu.dto.OperationResult;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Lobby {
    private final String lobbyID;
    private final Map<String, Client> players = new HashMap<>();
    private final Host host;

    private boolean locked;

    private UUID gameID;
    private final Map<String, String> userToPlayer = new HashMap<>();
    private final Map<String, String> playerToUser = new HashMap<>();
    private int nextPlayerID = 1;

    ExecutorService broadcastPool = Executors.newCachedThreadPool();

    public Lobby(String lobbyID, Client creator, Host host) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(host, "host must not be null");
        this.lobbyID = lobbyID;
        this.host = host;
        addPlayer(creator);
        //TODO: Make playerIDs and reverse
    }

    public OperationResult addPlayer(Client client) {
        if (locked) {
            return new OperationResult("lobby_locked");
        } else {
            players.put(nextPlayerID + "", client);
            userToPlayer.put(client.getUserID(), nextPlayerID + "");
            playerToUser.put(nextPlayerID + "", client.getUserID());
            nextPlayerID++;
            return  new OperationResult("success");
        }
    }

    public void removePlayer(Client client) {
        players.values().remove(client);
    } //TODO: handle maps

    public void startGame() {
        this.gameID = host.startGame(players.size(), 10); // TODO: Change the boardsize to be decided by the client
        this.locked = true;
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "game");

        ObjectNode payload = JsonUtil.createObjectNode();
        payload.put("action", "start");

        root.set("payload", payload);
        broadcastToClients(root);
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
        root.put("type", "game");
        root.set("payload", json.get("payload"));

        switch (json.get("delivery").asText()) {
            case "DIRECT":
                Client client = players.get(json.get("meta").get("player").get("playerID").asText());
                client.handleMessage(root);
                break;
            case "BROADCAST":
                broadcastToClients(root);
                break;
            default:
                break;
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

}
