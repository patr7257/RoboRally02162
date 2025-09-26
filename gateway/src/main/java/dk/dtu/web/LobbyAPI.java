package dk.dtu.web;

import com.fasterxml.jackson.databind.JsonNode;
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
        Client creator = serverRegistry.getClients().get(username); // TODO: make check that person is connected to websocket (essentially
        // check if in clients
        Lobby lob = LobbyFactory.createLobby(creator, serverRegistry.getHost());
        serverRegistry.getLobbies().put(lob.getLobbyID(), lob);
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
        // TODO: add error checking
    }

    @PostMapping("/lobby/join")
    public ResponseEntity<String> joinLobby(@RequestBody JsonNode json) {
        String username = json.get("username").asText();
        // TODO: add error handling
        String lobbyID = json.get("lobbyID").asText();
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        Client client = serverRegistry.getClients().get(username);
        Lobby lob = serverRegistry.getLobbies().get(lobbyID);
        lob.addPlayer(client);
        // TODO: add check if lobby is full and return success/failure message
        // success message
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
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
    public ResponseEntity<String> seeLobbies() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Lobby lobby : serverRegistry.getLobbies().values()) { // TODO: Do this in Lobby
            Map<String, Object> lobbyInfo = new HashMap<>();
            lobbyInfo.put("lobbyID", lobby.getLobbyID());
            result.add(lobbyInfo);
        }

        String json = JsonUtil.toJson(result);

        return ResponseEntity.ok(json);
    }
}
