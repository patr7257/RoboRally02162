package dk.dtu;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Consumer;

public class Lobby {
    private final String lobbyID;
    private final Map<String, Client> players = new HashMap<>();
    private final Host host;
    private final String gameID= "not set";
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

    public void startGame(Consumer<String> registerGameId) {
        //TODO: REST call host
        //TODO: set game ID
        registerGameId.accept(gameID);
        //TODO: message clients. (type=game, action=start?)
    }

    public void handleClientMessage(String userID, JsonNode payload) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("gameID", this.gameID);
        root.put("playerID", userToPlayer.get(userID));
        root.set("payload", payload);
        host.handleMessage(root);
    }

    public void handleHostMessage(JsonNode json) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "game");
        root.set("payload", json.get("payload"));
        switch (json.get("type").asText()) {
            case "direct":
                Client client = players.get(json.get("playerID").asText());
                client.handleMessage(root);
                break;
            case "broadcast":
                players.values().forEach(c -> c.handleMessage(root));
//                for (Client c : players.values()) {
//                    c.handleMessage(root);
//                }
                break;
            default:
                break;
        }
    }
}
