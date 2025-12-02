package dk.dtu.web;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.dto.OperationResult;
import dk.dtu.dto.ViewAllGamesInfoJson;
import dk.dtu.interfaces.GameDatabase;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.database.DynamicGameDatabase;

import dk.dtu.shared.AuthManager;
import dk.dtu.shared.GameService;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.APIUtil;
import dk.dtu.util.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */

@RestController
@RequestMapping("/api")
public class GameHandler {

    private final ServerManager serverManager;
    private final GameService gameService;

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public GameHandler(ServerManager serverManager, GameService gameService) {
        this.serverManager = serverManager;
        this.gameService = gameService;
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @GetMapping("/game/seeSavedGames")
    public ResponseEntity<String> seeSavedGames() {
        String userID = APIUtil.getCallerID();
        List<String> saveIDs = gameService.getSavedGames(userID);
        List<Map<String, String>> gameMap = new ArrayList<>();
        for (String id : saveIDs) {
            String lobbyName = gameService.getLobbyName(id).asText();
            gameMap.add(Map.of(
                    "saveID", id,
                    "lobbyName", lobbyName
                    )
            );
        }
        String response = JsonUtil.toJson(gameMap);
        return ResponseEntity.ok(response);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @PostMapping("/game/loadGame")
    public ResponseEntity<String> loadGame(@RequestBody JsonNode json) {
        String userID = APIUtil.getCallerID();
        String saveID = json.get("saveID").asText();
        Client c = serverManager.getClient(userID);

        JsonNode users = gameService.getUsers(saveID);
        String lobbyName = gameService.getLobbyName(saveID).asText();
        Map<String, String> userToPlayer = JsonUtil.toMap(users.toString());

        Lobby lob = serverManager.getLoadedLobbyFromSaveID(saveID);
        if (lob == null) {
            lob = serverManager.recreateLobby(c, lobbyName, userToPlayer, UUID.fromString(saveID));
            return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
        } else {
            OperationResult result = lob.addPlayer(serverManager.getClient(userID));
            if ("success".equals(result.getStatus())) {
                return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID());
            } else if ("lobby_locked".equals(result.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOBBY_LOCKED");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("UNKNOWN_ERROR");
            }
        }
    }

    /**
     * Endpoint to delete a saved game based on its saveID.
     * This method expects a JSON payload with a "saveID" field.
     * It will attempt to delete the game from the database.
     *
     * @param json JSON object containing the "saveID" of the game to delete.
     * @return ResponseEntity with status 200 (OK) if deletion succeeds,
     * or 403 (FORBIDDEN) if there was an error during deletion.
     *
     * @author Benjamin Benyo Endahl Hansen
     */
    @PostMapping("/game/deleteSavedGame")
    public ResponseEntity<String> deleteSavedGame(@RequestBody JsonNode json) {
        String userID = APIUtil.getCallerID();
        String saveID = json.get("saveID").asText();

        try {
            if (gameService.checkUserInGame(userID, saveID)) {
                gameService.deleteSavedGame(this.serverManager, saveID);
                return ResponseEntity.status(HttpStatus.OK).body("Game Deleted");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not in game");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error in deleting game");
        }
    }

    /**
     * Endpoint to fetch a list of all saved games and its corresponding gameInfo from the database.
     * Each game includes its saveID, number of players, and winner username (if any).
     *
     * @return 200 OK with a list of ViewAllGamesInfoJson objects
     *
     * @author Benjamin Benyo Endahl Hansen
     */
    @GetMapping("/game/seeAllGames")
    public ResponseEntity<List<ViewAllGamesInfoJson>> seeAllGames() {
        Map<String, JsonNode> allGames = gameService.getAllGames();

        List<ViewAllGamesInfoJson> cleaned = allGames.entrySet()
                .stream()
                .map(entry -> {
                    JsonNode root = entry.getValue();

                    JsonNode usersMap = root.path("users");
                    JsonNode snap = root.path("gameSnapshot").path("snapshotPayload");
                    int playerCount = snap.path("robots").size();

                    JsonNode winnerNode = snap.path("game").path("winner");
                    String winnerRobotID = winnerNode.isNull() ? null : winnerNode.asText();

                    String lobbyName = root.path("lobbyName").asText();

                    // Find winner UUID from users map
                    String winnerUUID = null;
                    if (winnerRobotID != null && usersMap.isObject()) {
                        Iterator<String> fieldNames = usersMap.fieldNames();
                        while (fieldNames.hasNext()) {
                            String uuid = fieldNames.next();
                            String pid = usersMap.get(uuid).asText();
                            if (pid.equals(winnerRobotID)) {
                                winnerUUID = uuid;
                                break;
                            }
                        }
                    }
                    String winnerUsername = null;
                    if (winnerUUID != null) {
                        winnerUsername = serverManager.getUsernameFromUUID(winnerUUID);
                    }

                    return new ViewAllGamesInfoJson(
                            lobbyName,
                            playerCount,
                            winnerUsername
                    );
                })
                .toList();
        return ResponseEntity.ok(cleaned);
    }
}