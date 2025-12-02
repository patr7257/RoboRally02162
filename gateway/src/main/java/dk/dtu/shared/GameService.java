package dk.dtu.shared;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.model.Lobby;
import dk.dtu.model.database.DynamicGameDatabase;
import dk.dtu.util.JsonUtil;
import org.springframework.stereotype.Service;
import dk.dtu.interfaces.GameDatabase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GameService {
    private final GameDatabase gameDatabase;

    public GameService(DynamicGameDatabase gameDatabase) {
        this.gameDatabase = gameDatabase;
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public void saveGame(ServerManager serverManager, Lobby lob) {
        try {
            JsonNode gameSnapshot = lob.saveGame();
            Map<String, String> userToPlayer = lob.getUserToPlayer();
            String lobbyName = lob.getLobbyName();
            Map<String, Object> game = Map.of(
                    "lobbyName", lobbyName,
                    "users", userToPlayer,
                    "gameSnapshot", gameSnapshot
            );
            JsonNode gameInfo = JsonUtil.toTree(game);

            for (String u : userToPlayer.keySet()) {
                gameDatabase.saveGame(u, gameInfo, lob.getSaveID().toString());
            }
            lob.notifyGameSaved(true);
            serverManager.notifyClientsOfUpdates("games", "updatedGames");
        } catch (Exception e) {
            lob.notifyGameSaved(false);
            e.printStackTrace();
        }
    }

    public List<String> getSavedGames(String userID) {
        return gameDatabase.getSavedGames(userID);
    }

    public JsonNode getUsers(String saveID) {
        return gameDatabase.getUsers(saveID);
    }

    public boolean checkUserInGame(String userID, String saveID) {
        return gameDatabase.checkUserInGame(userID, saveID);
    }

    public void deleteSavedGame(ServerManager serverManager, String saveID) {
        gameDatabase.deleteSavedGame(saveID);
        serverManager.notifyClientsOfUpdates("games", "updatedGames");
    }

    public Map<String, JsonNode> getAllGames() {
        return gameDatabase.getAllGames();
    }

    public JsonNode getLobbyName(String saveID) {
        return gameDatabase.getLobbyName(saveID);
    }

}
