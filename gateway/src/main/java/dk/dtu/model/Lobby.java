package dk.dtu.model;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;

import java.util.*;

public class Lobby {
    private final String lobbyID;
    private final Map<String, Client> players = new HashMap<>();
    private final Host host;
    private String gameID = "not set";
    private final Map<String, String> userToPlayer = new HashMap<>();
    private final Map<String, String> playerToUser = new HashMap<>();
    private int nextPlayerID = 1;


    public Lobby(String lobbyID, Client creator, Host host) {
        this.lobbyID = lobbyID;
        this.host = host;
        addPlayer(creator);
        //TODO: Make playerIDs and reverse
    }

    public String getLobbyID() {
        return lobbyID;
    }

    public Map<String, Client> getPlayers() {
        return players;
    }

    public void addPlayer(Client client) {
        players.put(nextPlayerID + "", client);
        userToPlayer.put(client.getUserID(), nextPlayerID + "");
        playerToUser.put(nextPlayerID + "", client.getUserID());
        nextPlayerID++;
    }

    public void removePlayer(Client client) {
        players.remove(client);
    } //TODO: handle maps

    public void startGame(String gameID) {
        this.gameID = gameID;
        //TODO: message clients. (type=game, action=start?)
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "game");

        ObjectNode payload = JsonUtil.createObjectNode();
        payload.put("action", "start");

        root.set("payload", payload);
        players.values().forEach(c -> c.handleMessage(root));
    }

    public void handleClientMessage(String userID, JsonNode payload) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("gameID", this.gameID);
        root.put("playerID", Integer.parseInt(userToPlayer.get(userID)));
        root.set("payload", payload);
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
                //players.values().forEach(c -> c.handleMessage(root));
                for (Client c : players.values()) {
                    c.handleMessage(root);
                }
                break;
            default:
                break;
        }
    }
}
