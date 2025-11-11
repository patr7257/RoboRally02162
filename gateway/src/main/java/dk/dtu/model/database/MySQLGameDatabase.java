package dk.dtu.model.database;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.interfaces.GameDatabase;
import dk.dtu.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 @author Karl Johannes Agerbo
 */

@Service("mysqlGameDatabase")
public class MySQLGameDatabase implements GameDatabase {

    private static final String URL = "jdbc:mysql://localhost:3306/RoboRallyDatabase";
    private static final String USER = "RoboRallyUser";
    private static final String PASSWORD = "RoboRallyDatabaseUser";

    /**
     @author Karl Johannes Agerbo
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     @author Karl Johannes Agerbo
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
     @author Karl Johannes Agerbo
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
     @author Karl Johannes Agerbo
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
     @author Karl Johannes Agerbo
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
     @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getUsers(String saveID) {
        JsonNode snapshot = getGameSnapshot(saveID);
        return snapshot != null ? snapshot.get("users") : null;
    }
}
