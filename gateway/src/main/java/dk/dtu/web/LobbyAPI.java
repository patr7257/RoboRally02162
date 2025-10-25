package dk.dtu.web;

/*
Author(s): Karl, Benjamin, Niklas
 */

import com.fasterxml.jackson.databind.JsonNode;

import dk.dtu.dto.LobbyJson;
import dk.dtu.dto.OperationResult;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api")
public class LobbyAPI {
    private final ServerManager serverManager;

    public LobbyAPI(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @PostMapping("/lobby/create") // returns lobbyID.
    public ResponseEntity<String> createLobby(@RequestBody JsonNode json) { // TODO: add authorization
        String username = json.get("username").asText();
        if(username.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("USERNAME_IS_EMPTY");
        }
        Client creator = serverManager.getClient(username); // TODO: make check that person is connected to websocket
        if(creator == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("CLIENT_IS_NULL");
        }
        // check if in clients

        Lobby lob = serverManager.createLobby(creator);

        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
    }

    @PostMapping("/lobby/join")
    public ResponseEntity<String> joinLobby(@RequestBody JsonNode json) {
        String lobbyID = json.get("lobbyID").asText();
        if(lobbyID.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_ID_IS_EMPTY");
        }
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        Lobby lob = serverManager.getLobbyFromID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        String username = json.get("username").asText();
        if(username.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("USERNAME_IS_EMPTY");
        }

        Client client = serverManager.getClient(username);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }

        if(lob.isOccupied()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_IS_FULL");
        }

        OperationResult result = lob.addPlayer(client);
        if ("success".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
        } else if ("lobby_locked".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_LOCKED");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("UNKNOWN_ERROR");
        }
    }

    @PostMapping("/lobby/start") // TODO: add check that websocket connection is running
    public void startLobby(@RequestBody JsonNode json) {
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        String lobbyID = json.get("lobbyID").asText();
        // TODO: add valid ID checking
        Lobby lob = serverManager.getLobbyFromID(lobbyID);
        lob.startGame();

    }

    @GetMapping("/lobby/seeLobbies")
    public ResponseEntity<String> seeLobbies() { //only re
        List<LobbyJson> result = new ArrayList<>();
        for (Lobby lobby : serverManager.getLobbiesListCopy().stream().filter(entry -> !entry.isLocked()).toList()) {
                result.add(lobby.asJson());
        }
        String json = JsonUtil.toJson(result);
        return ResponseEntity.ok(json);
    }

    @PostMapping("lobby/leave")
    public ResponseEntity<String> leaveLobby(@RequestBody JsonNode json) {
        String username = json.get("username").asText(); //TODO: change to userID
        Client client = serverManager.getClient(username);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        String lobbyID = json.get("lobbyID").asText();
        Lobby lob = serverManager.getLobbyFromID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        OperationResult operationResult = lob.removeClientByUID(client.getUserID());
        String status = operationResult.getStatus();
        return switch (status) {
            case "success", "lobby_empty" -> ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
            case "user_not_in_lobby" -> ResponseEntity.status(HttpStatus.CONFLICT).body("USER_NOT_IN_LOBBY");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UNKNOWN_ERROR");
        };



    }
}
