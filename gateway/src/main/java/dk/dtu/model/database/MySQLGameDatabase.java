package dk.dtu.model.database;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.interfaces.GameDatabase;
import dk.dtu.util.JsonUtil;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Karl Johannes Agerbo
 */

@Service("mysqlGameDatabase")
public class MySQLGameDatabase implements GameDatabase {

    /**
     * @author Karl Johannes Agerbo
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DatabaseCredentials.DBURL, DatabaseCredentials.USER, DatabaseCredentials.PASSWORD);
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public void saveGame(String userID, JsonNode snapshot, String saveID) {
        String insertGame = "INSERT IGNORE INTO games (SaveID, Snapshot) VALUES (?, ?)";
        String updateGame = "UPDATE games SET Snapshot = ? WHERE SaveID = ?";
        String insertSave = "INSERT IGNORE INTO saves (UserID, SaveID) VALUES (?, ?)";

        try (Connection conn = connect();
             PreparedStatement insertGameStmt = conn.prepareStatement(insertGame);
             PreparedStatement updateGameStmt = conn.prepareStatement(updateGame);
             PreparedStatement saveStmt = conn.prepareStatement(insertSave)) {

            insertGameStmt.setString(1, saveID);
            insertGameStmt.setString(2, snapshot.toString());
            insertGameStmt.executeUpdate();

            updateGameStmt.setString(1, snapshot.toString());
            updateGameStmt.setString(2, saveID);
            updateGameStmt.executeUpdate();

            saveStmt.setString(1, userID);
            saveStmt.setString(2, saveID);
            saveStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("MySQL saveGame failed", e);
        }
    }



    /**
     * Deletes a saved game from the database.
     *
     * This method removes the game with a specified saveID from the "games" table.
     * Since the foreign key in the "saves" table is configured with ON DELETE CASCADE,
     * all related entries in "saves" will automatically be deleted,
     * so we only need to delete from the "games" table.
     *
     * @param saveID the identifier of the saved game to delete
     * @throws RuntimeException if the SQL operation fails
     *
     * @author Benjamin Benyo Endahl Hansen
     */

    @Override
    public void deleteSavedGame(String saveID) {
        String deleteGame = "DELETE FROM games WHERE SaveID = ?";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(deleteGame)) {
            stmt.setString(1, saveID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("MySQL deleteSavedGame failed", e);
        }
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Override
    public boolean checkUserInGame(String userID, String saveID) {
        String game = "SELECT userID, saveID FROM saves WHERE UserID = ? AND SaveID = ?";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(game)) {
            stmt.setString(1, userID);
            stmt.setString(2, saveID);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("MySQL checkUserInGame failed", e);
        }
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public List<String> getSavedGames(String userID) {
        List<String> saveIDs = new ArrayList<>();
        String query = "SELECT SaveID FROM saves WHERE UserID = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    saveIDs.add(rs.getString("SaveID"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("MySQL getSavedGames failed", e);
        }

        return saveIDs;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public boolean wipeGameDatabase() {
        String deleteSaves = "DELETE FROM saves";
        String deleteGames = "DELETE FROM games";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(deleteSaves);
            stmt.executeUpdate(deleteGames);
            return true;

        } catch (SQLException e) {
            throw new RuntimeException("MySQL wipeGameDatabase failed", e);
        }
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getGameSnapshot(String saveID) {
        String query = "SELECT Snapshot FROM games WHERE SaveID = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, saveID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("Snapshot");
                    return JsonUtil.parser(json);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("MySQL getSavedGames failed", e);
        }

        return null;
    }



    /**
     * @author Benjamin Benyo Endahl Hansen
     */
    @Override
    public Map<String, JsonNode> getAllGames() {
        Map<String, JsonNode> saveSnapshots = new HashMap<>();
        String query = """
        SELECT * FROM games
    """;

        ObjectMapper objectMapper = new ObjectMapper();

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String saveID = rs.getString("SaveID");
                String jsonString = rs.getString("Snapshot");
                JsonNode jsonNode = objectMapper.readTree(jsonString);
                saveSnapshots.put(saveID, jsonNode);
            }

        } catch (SQLException e) {
            throw new RuntimeException("MySQL getAllSaveSnapshots failed", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON snapshot", e);
        }

        return saveSnapshots;
    }

    @Override
    public JsonNode getLobbyName(String saveID) {
        JsonNode snapshot = getGameSnapshot(saveID);
        return snapshot != null ? snapshot.get("lobbyName") : null;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getWinner(String saveID) {
        JsonNode snapshot = getGameSnapshot(saveID);
        return snapshot != null ? snapshot.get("gameSnapshot").get("winner") : null;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getUsers(String saveID) {
        JsonNode snapshot = getGameSnapshot(saveID);
        return snapshot != null ? snapshot.get("users") : null;
    }
}
