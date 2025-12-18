package dk.dtu.model.database;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.interfaces.GameDatabase;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 @author Bjarke Søderhamn Petersen
 @author Benjamin Benyo Endahl Hansen
 @author Karl Johannes Agerbo
 */

@Service("localGameDatabase")
public class LocalGameDatabase implements GameDatabase {

    private final Map<String, List<String>> saveIDsFromUserID = new HashMap<>();
    private final Map<String, JsonNode> gamesFromSaveID = new HashMap<>();

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Override
    public void saveGame(String userID, JsonNode gameInfo, String saveID) {
        List<String> saveIDs = saveIDsFromUserID.computeIfAbsent(userID, _ -> new ArrayList<>());
        if (!saveIDs.contains(saveID)) {
            saveIDs.add(saveID);
        }
        gamesFromSaveID.put(saveID, gameInfo);
    }

    /**
     * Deletes a saved game for all users who have been part of the specific game,
     * based on the saveID of that game.
     *
     * @param saveID The saveID of the game to delete.
     *
     @author Benjamin Benyo Endahl Hansen
     */

    @Override
    public void deleteSavedGame(String saveID) {
        List<String> users = this.getUsersBySaveID(saveID);

        for (String userID : users) {
            List<String> saveIDs = saveIDsFromUserID.get(userID);

            if (saveIDs != null) {
                saveIDs.remove(saveID);
                if (saveIDs.isEmpty()) {
                    saveIDsFromUserID.remove(userID);
                }
            }
        }
        gamesFromSaveID.remove(saveID);
    }

    @Override
    public boolean checkUserInGame(String userID, String saveID) {
        List<String> saveIDs = saveIDsFromUserID.get(userID);
        return saveIDs != null && saveIDs.contains(saveID);
    }

    /**
     * Returns a list of all users ID's that have been part of a specific game,
     * based on the saveID of that game.
     *
     * @param saveID The save ID of the game to look up.
     * @return A list of user IDs who have the specified save ID.
     *
     @author Benjamin Benyo Endahl Hansen
     */

    public List<String> getUsersBySaveID(String saveID) {
        List<String> users = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : saveIDsFromUserID.entrySet()) {
            if (entry.getValue().contains(saveID)) {
                users.add(entry.getKey());
            }
        }
        return users;
    }


    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public List<String> getSavedGames(String userID) {
        return saveIDsFromUserID.get(userID) == null ? new ArrayList<>() : saveIDsFromUserID.get(userID);
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getUsers(String saveID) {
        return gamesFromSaveID.get(saveID).get("users");
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public JsonNode getGameSnapshot(String saveID) {
        return gamesFromSaveID.get(saveID);
    }

    /**
     * @author Benjamin Benyo Endahl Hansen
     */
    @Override
    public Map<String, JsonNode> getAllGames() {
        return new HashMap<>(gamesFromSaveID);
    }

    @Override
    public JsonNode getLobbyName(String saveID) {
        return gamesFromSaveID.get(saveID).get("lobbyName");
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getWinner(String saveID) {
        return gamesFromSaveID.get(saveID).get("gameSnapshot").get("winner");
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public synchronized boolean wipeGameDatabase() {
        gamesFromSaveID.clear();
        saveIDsFromUserID.clear();

        return true;
    }


}
