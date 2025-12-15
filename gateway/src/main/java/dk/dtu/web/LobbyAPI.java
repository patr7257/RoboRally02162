package dk.dtu.web;


import com.fasterxml.jackson.databind.JsonNode;

import dk.dtu.dto.*;
import dk.dtu.interfaces.GameDatabase;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.database.DynamicGameDatabase;
import dk.dtu.service.DemoService;
import dk.dtu.shared.AuthManager;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.APIUtil;
import dk.dtu.util.JsonUtil;
import dk.dtu.service.BoardTemplateService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Niklas Emil Lysdal
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 * @author Asger Allin Jensen
 * @author Kajsa Alice Ulrika Berlstedt
 * @author Patrick Røbel
 */
@RestController
@RequestMapping("/api/lobby")
public class LobbyAPI {
    private final ServerManager serverManager;
    private final GameDatabase gameDatabase;
    private final AuthManager authManager;
    private final BoardTemplateService boardTemplateService;
    private final DemoService demoService;

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Patrick Røbel
     */
    public LobbyAPI(ServerManager serverManager, DynamicGameDatabase gameDatabase, AuthManager authManager,BoardTemplateService boardTemplateService, DemoService demoService) {
        this.serverManager = serverManager;
        this.gameDatabase = gameDatabase;
        this.authManager = authManager;
        this.boardTemplateService = boardTemplateService;
        this.demoService = demoService;
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @PostMapping("/create") // returns lobbyID.
    public ResponseEntity<String> createLobby(@RequestBody JsonNode json) {
        String userID = APIUtil.getCallerID();

        Client creator = serverManager.getClient(userID);
        System.out.println("CLIENT with id " + userID + " IS: " + creator);
        if (creator == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("MISSING_WEBSOCKET_CONNECTION");
        }

        if (!json.has("lobbyName") || json.get("lobbyName").asText().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("MISSING_LOBBY_NAME");
        }
        String lobbyName = json.get("lobbyName").asText();
        //TODO: check valid lobbyName

        if (!json.has("capacity") || json.get("capacity").asText().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("MISSING_CAPACITY");
        }
        int capacity;
        try {
            capacity = json.get("capacity").asInt();
            if (capacity < 1 || capacity > 6) {
                throw new Exception("INVALID_CAPACITY");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_CAPACITY");
        }

        // Get board template name if provided (default to "Random")
        String boardTemplateName = "Random";
        if (json.has("boardTemplate") && !json.get("boardTemplate").asText().isBlank()) {
            boardTemplateName = json.get("boardTemplate").asText();
        }

        Lobby lob;
        try {
            lob = serverManager.createLobby(creator, lobbyName, capacity);
            lob.setBoardTemplateName(boardTemplateName); // Store the template name
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("LOBBY_NAME_ALREADY_EXISTS");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @PostMapping("/join")
    public ResponseEntity<String> joinLobby(@RequestBody LobbyRequest req) {
        String lobbyID = req.lobbyID;
        if (lobbyID.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_ID_IS_EMPTY");
        }

        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }

        String userID = APIUtil.getCallerID();
        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }

        if (lob.isOccupied()) {
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

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Patrick Røbel
     */
    @PostMapping("/start") // TODO: add check that websocket connection is running
    public ResponseEntity<String> startLobby(@RequestBody LobbyRequest req) {
        String lobbyID = req.lobbyID;
        String userID = APIUtil.getCallerID();

        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        if (!lob.hasParticipant(userID)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_IN_LOBBY");
        }
        try {
            if (lob.isLoadedLobby()) {
                System.out.println("Starting loaded game");
                JsonNode snapshot = gameDatabase.getGameSnapshot(lob.getSaveID().toString());
                lob.startGame(snapshot.get("gameSnapshot"));
            } else {
                // Use the template name stored in the lobby
                String templateName = lob.getBoardTemplateName();
                System.out.println("Template name from lobby: " + templateName);

                // If not "Random", load the template
                if (templateName != null && !templateName.equals("Random")) {
                    JsonNode boardTemplate = boardTemplateService.getTemplate(templateName);

                    if (boardTemplate == null) {
                        System.out.println("ERROR: Template not found: " + templateName);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TEMPLATE_NOT_FOUND");
                    }

                    System.out.println("Starting game with template: " + templateName);
                    lob.startGameWithTemplate(boardTemplate);
                } else {
                    // Start with random board
                    System.out.println("Starting game with random board");
                    lob.startGame(null);
                }
            }
            System.out.println("=== GAME STARTED SUCCESSFULLY ===");
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            System.out.println("ERROR starting game: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }

    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     */
    @GetMapping("/seeLobbies")
    public ResponseEntity<String> seeLobbies() { //only re
        List<LobbyPublicJson> result = new ArrayList<>();
        for (Lobby lobby : serverManager.getLobbiesListCopy()) {
            result.add(lobby.asPublicJson(APIUtil.getCallerID()));
        }
        result.sort(Comparator.comparing(LobbyPublicJson::joinable, Comparator.reverseOrder()).thenComparing(LobbyPublicJson::isRunning, Comparator.reverseOrder()));
        String response = JsonUtil.toJson(result);

        return ResponseEntity.ok(response);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @PostMapping("/lobbyInfo")
    public ResponseEntity<?> getLobbyInfo(@RequestBody LobbyRequest req) {
        String userID = APIUtil.getCallerID();

        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        String lobbyID = req.lobbyID;
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        if (!lob.hasParticipant(userID)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_IN_LOBBY");
        }
        return ResponseEntity.status(HttpStatus.OK).body(lob.asPrivateJson());
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @PostMapping("/leave")
    public ResponseEntity<String> leaveLobby(@RequestBody LobbyRequest req) {
        String userID = APIUtil.getCallerID();
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

    /**
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */
    @PostMapping("/markReady")
    public ResponseEntity<String> markReady(@RequestBody LobbyRequest req) {
        String userID = APIUtil.getCallerID();
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
        return switch (status) {
            case "success" -> ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
            case "user_not_in_lobby" -> ResponseEntity.status(HttpStatus.CONFLICT).body("USER_NOT_IN_LOBBY");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UNKNOWN_ERROR");
        };
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */
    @PostMapping("/markNotReady")
    public ResponseEntity<String> markNotReady(@RequestBody LobbyRequest req) {
        String userID = APIUtil.getCallerID();
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

    /**
     * @author Kajsa Alice Ulrika Berlstedt
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */
    @PostMapping("/getRobot")
    public ResponseEntity<String> getRobot(@RequestBody LobbyRequest req) {
        String lobbyID = req.lobbyID;
        String userID = APIUtil.getCallerID();
        Client client = serverManager.getClient(userID);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_CONNECTED");
        }
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        if (lob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LOBBY_NOT_FOUND");
        }
        if (!lob.hasParticipant(userID)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_IN_LOBBY");
        }
        Map<String, String> result = lob.getUsernamePlayerIDMaps();
        String json = JsonUtil.toJson(result);
        return ResponseEntity.ok(json);
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @PostMapping("/createAndStartDemo")
    public ResponseEntity<String> createAndStartDemo(@RequestBody JsonNode demoTemplateName) {
        String userID = APIUtil.getCallerID();

        Client creator = serverManager.getClient(userID);
        System.out.println("CLIENT with id " + userID + " IS: " + creator);
        if (creator == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("MISSING_WEBSOCKET_CONNECTION");
        }

        String demoName = demoTemplateName.get("demoTemplate").asText();
        if (demoName == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("MISSING_DEMO_TEMPLATE");
        }

        Lobby lob = serverManager.createDemoLobby(creator);

        JsonNode demoTemplate = null;
        try {
            demoTemplate = demoService.loadDemoTemplate(demoName);
        } catch (IOException e) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOAD_DEMO_FAILED");
        }

        try {
            lob.startGame(demoTemplate);
        } catch (Exception e) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body("START_GAME_FAILED");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
    }

}
