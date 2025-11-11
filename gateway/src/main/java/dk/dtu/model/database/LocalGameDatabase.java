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
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Override
    public List<String> getSavedGames(String userID) {
        return saveIDsFromUserID.get(userID) == null ? new ArrayList<>() : saveIDsFromUserID.get(userID);
    }

    /**
     @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getUsers(String saveID) {
        return gamesFromSaveID.get(saveID).get("users");
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Override
    public JsonNode getGameSnapshot(String saveID) {
        return gamesFromSaveID.get(saveID);
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Override
    public synchronized boolean wipeGameDatabase() {
        gamesFromSaveID.clear();
        saveIDsFromUserID.clear();

        return true;
    }


}
