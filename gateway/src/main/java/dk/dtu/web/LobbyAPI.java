package dk.dtu.web;

/*
Author(s): Karl, Benjamin, Niklas
 */

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.dto.OperationResult;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.shared.ServerRegistry;
import dk.dtu.util.JsonUtil;
import dk.dtu.util.LobbyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api")
public class LobbyAPI {
    private final ServerRegistry serverRegistry;
    public LobbyAPI(ServerRegistry serverRegistry) {
        this.serverRegistry = serverRegistry;
    }
    @PostMapping("/lobby/create") // returns lobbyID.
    public ResponseEntity<String> createLobby(@RequestBody JsonNode json) { // TODO: add authorization
        String username = json.get("username").asText();
        if(username.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("USERNAME_IS_NULL");
        }
        Client creator = serverRegistry.getClients().get(username); // TODO: make check that person is connected to websocket (essentially
        if(creator == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("CLIENT_IS_NULL");
        }
        // check if in clients

        Lobby lob = LobbyFactory.createLobby(creator, serverRegistry.getHost());
        serverRegistry.getLobbies().put(lob.getLobbyID(), lob);
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
    }

    @PostMapping("/lobby/join")
    public ResponseEntity<String> joinLobby(@RequestBody JsonNode json) {
        String lobbyID = json.get("lobbyID").asText();
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        Lobby lob = serverRegistry.getLobbies().get(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        String username = json.get("username").asText();
        // TODO: add error handling


        Client client = serverRegistry.getClients().get(username);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        OperationResult result = lob.addPlayer(client);
        if ("success".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
        } else if ("lobby_locked".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_LOCKED");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("UNKNOWN_ERROR");
        }
        // TODO: add check if lobby is full and return success/failure message
        // success message
    }

    @PostMapping("/lobby/start") // TODO: add check that websocket connection is running
    public void startLobby(@RequestBody JsonNode json) {
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        String lobbyID = json.get("lobbyID").asText();
        // TODO: add valid ID checking
        Lobby lob = serverRegistry.getLobbies().get(lobbyID);
        lob.startGame();
        serverRegistry.getGameToLobby().put(lob.getGameID().toString(), lob.getLobbyID());
    }

    @GetMapping("/lobby/seeLobbies")
    public ResponseEntity<String> seeLobbies() { //only re
        List<Map<String, Object>> result = new ArrayList<>();
        for (Lobby lobby : serverRegistry.getLobbies().values()) {
            if (!lobby.isLocked()) {
                Map<String, Object> lobbyInfo = new HashMap<>();
                lobbyInfo.put("lobbyID", lobby.getLobbyID());
                result.add(lobbyInfo);   // TODO: create "asJson" in lobby class.
            }
        }

        String json = JsonUtil.toJson(result);

        return ResponseEntity.ok(json);
    }

    @PostMapping("lobby/leave")
    public ResponseEntity<String> leaveLobby(@RequestBody JsonNode json) {
        String username = json.get("username").asText(); //TODO: change to userID
        Client client = serverRegistry.getClients().get(username);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        String lobbyID = json.get("lobbyID").asText();
        Lobby lob = serverRegistry.getLobbies().get(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        OperationResult operationResult = lob.removeClientByUID(client.getUserID());
        String status = operationResult.getStatus();
        switch (status) {
            case "success": {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
            }
            case "lobby_empty": {
                serverRegistry.getLobbies().remove(lob.getLobbyID());
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
            }
            case "user_not_in_lobby": {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("USER_NOT_IN_LOBBY");
            }

            default: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UNKNOWN_ERROR");
        }



    }
}
