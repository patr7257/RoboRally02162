package dk.dtu.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */

public interface GameDatabase {
    void saveGame(String userID, JsonNode snapshot, String saveID);
    List<String> getSavedGames(String userID);
    JsonNode getUsers(String saveID);
    boolean wipeGameDatabase();
    JsonNode getGameSnapshot(String saveID);
}
