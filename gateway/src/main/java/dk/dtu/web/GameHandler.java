package dk.dtu.web;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.dto.OperationResult;
import dk.dtu.interfaces.GameDatabase;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.database.DynamicGameDatabase;

import dk.dtu.shared.AuthManager;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.APIUtil;
import dk.dtu.util.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private final GameDatabase gameDatabase;
    private final ServerManager serverManager;

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public GameHandler(DynamicGameDatabase gameDatabase, ServerManager serverManager) {
        this.gameDatabase = gameDatabase;
        this.serverManager = serverManager;
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @PostMapping("/game/save")
    public ResponseEntity<String> saveGame(@RequestBody JsonNode json) {
        String lobbyID = json.get("lobbyID").asText();

        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);

        try {
            JsonNode gameSnapshot = lob.saveGame();
            Map<String, String> userToPlayer = lob.getUserToPlayer();

            Map<String, Object> game = Map.of(
                    "users", userToPlayer,
                    "gameSnapshot", gameSnapshot
            );

            JsonNode gameInfo = JsonUtil.toTree(game);

            for (String u : userToPlayer.keySet()) {
                gameDatabase.saveGame(u, gameInfo, lob.getSaveID().toString());
            }

            //System.out.println("gameSnapShot: \n" + gameDatabase.getGameSnapshot(lob.getSaveID().toString()));

            lob.notifyGameSaved(true);

            return ResponseEntity.status(HttpStatus.OK).body("Game Saved");
        } catch (Exception e) {
            lob.notifyGameSaved(false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error in saving game");
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @GetMapping("/game/seeSavedGames")
    public ResponseEntity<String> seeSavedGames() {
        String userID = APIUtil.getCallerID();
        List<String> saveIDs = gameDatabase.getSavedGames(userID);
        List<Map<String, String>> gameMap = new ArrayList<>();
        for (String id : saveIDs) {
            gameMap.add(Map.of("saveID", id));
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

        JsonNode users = gameDatabase.getUsers(saveID);

        Map<String, String> userToPlayer = JsonUtil.toMap(users.toString());
        Lobby lob = serverManager.getLoadedLobbyFromSaveID(saveID);
        if (lob == null) {
            lob = serverManager.recreateLobby(c, userToPlayer, UUID.fromString(saveID));
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

}