package dk.dtu.web;

/*
Author(s): Karl, Benjamin, Niklas
 */

import com.fasterxml.jackson.databind.JsonNode;

import dk.dtu.dto.LobbyJson;
import dk.dtu.dto.LobbyUserRequest;
import dk.dtu.dto.OperationResult;
import dk.dtu.interfaces.GameDatabase;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.database.DynamicGameDatabase;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;

import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api")
public class LobbyAPI {
    private final ServerManager serverManager;
    private final GameDatabase gameDatabase;

    public LobbyAPI(ServerManager serverManager, DynamicGameDatabase gameDatabase) {
        this.serverManager = serverManager;
        this.gameDatabase = gameDatabase;
    }

    @PostMapping("/lobby/create") // returns lobbyID.
    public ResponseEntity<String> createLobby(@RequestBody JsonNode json) { // TODO: add authorization

        if (!json.has("userID")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_REQUEST_BODY");
        }
        String userID = json.get("userID").asText();
        if(userID.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("USERID_IS_EMPTY");
        }
        Client creator = serverManager.getClient(userID); // TODO: make check that person is connected to websocket
        if(creator == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("CLIENT_IS_NULL");
        }
        // check if in clients

        Lobby lob = serverManager.createLobby(creator);

        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
    }

    @PostMapping("/lobby/join")
    public ResponseEntity<String> joinLobby(@RequestBody LobbyUserRequest req) {
        String lobbyID = req.lobbyID;
        if(lobbyID.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_ID_IS_EMPTY");
        }
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        String userID = req.userID;
        if(userID.isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("USERID_IS_EMPTY");
        }

        Client client = serverManager.getClient(userID);
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
    public ResponseEntity<String> startLobby(@RequestBody JsonNode json) {
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        String lobbyID = json.get("lobbyID").asText();
        // TODO: add valid ID checking
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        try {
            if (lob.isLoadedLobby()) {
                JsonNode snapshot = gameDatabase.getGameSnapshot(lob.getSaveID().toString());
                lob.startGame(snapshot.get("gameSnapshot"));
            } else {
                lob.startGame(null);
            }
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }

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

    @PostMapping("/lobby/leave")
    public ResponseEntity<String> leaveLobby(@RequestBody LobbyUserRequest req) {
        String userID = req.userID;
        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        String lobbyID = req.lobbyID;
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
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

    /// @author Asger
    /// @author Niklas
    @PostMapping("/lobby/markReady")
    public ResponseEntity<String> markReady(@RequestBody LobbyUserRequest req) {
        String userID = req.userID;
        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        String lobbyID = req.lobbyID;
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        OperationResult operationResult = lob.playerMarkedAsReady(userID);
        String status = operationResult.getStatus();
        System.out.println("Status is: "+status);
        System.out.println("UserID: " + userID);
        return switch (status) {
            case "success" -> ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
            case "user_not_in_lobby" -> ResponseEntity.status(HttpStatus.CONFLICT).body("USER_NOT_IN_LOBBY");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UNKNOWN_ERROR");
        };
    }

    // @author Asger
    // @author Niklas
    @PostMapping("/lobby/markNotReady")
    public ResponseEntity<String> markNotReady(@RequestBody LobbyUserRequest req) {
        String userID = req.userID;
        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        String lobbyID = req.lobbyID;
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        OperationResult operationResult = lob.playerMarkedAsNotReady(userID);
        String status = operationResult.getStatus();
        return switch (status) {
            case "success" -> ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
            case "user_not_in_lobby" -> ResponseEntity.status(HttpStatus.CONFLICT).body("USER_NOT_IN_LOBBY");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UNKNOWN_ERROR");
        };
    }

    // @author Asger Allin Jensen
    // @author Kajsa Alice Ulrika Berlstedt
    @PostMapping("/lobby/getRobot")
    public ResponseEntity<String> getRobot(@RequestBody LobbyUserRequest req) {
        String lobbyID = req.lobbyID;
        String userID = req.userID;
        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        Map<String, String> result = lob.getUsernamePlayerIDMaps();
        System.out.println("api call");
        String json = JsonUtil.toJson(result);
        System.out.println(json);
        return ResponseEntity.ok(json);
    }
}
