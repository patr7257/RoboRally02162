package dk.dtu.model.database;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.interfaces.GameDatabase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */

@Service
public class DynamicGameDatabase implements GameDatabase {

    private final GameDatabase mysql;
    private final GameDatabase local;
    private boolean mysqlAvailable = true;

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    public DynamicGameDatabase(@Qualifier("mysqlGameDatabase") GameDatabase mysql,
                               @Qualifier("localGameDatabase") GameDatabase local) {
        this.mysql = mysql;
        this.local = local;
        this.mysqlAvailable = checkMySQLAvailable();
        if (!mysqlAvailable)
            System.err.println("MySQLGameDatabase not available, using local fallback database.");
    }

    /**
     * @author Karl Johannes Agerbo
     */
    private boolean checkMySQLAvailable() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/RoboRallyDatabase",
                "RoboRallyUser", "RoboRallyDatabaseUser")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * @author Karl Johannes Agerbo
     */
    private GameDatabase active() {
        return mysqlAvailable ? mysql : local;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    private void disableMySQL() {
        System.err.println("Switching to local fallback database due to MySQL error.");
        mysqlAvailable = false;
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public void saveGame(String userID, JsonNode snapshot, String saveID) {
        try {
            active().saveGame(userID, snapshot, saveID);
        } catch (Exception e) {
            disableMySQL();
            local.saveGame(userID, snapshot, saveID);
        }
    }


    /**
     * Deletes a saved game using the provided saveID.
     * This method first attempts to delete the game from the active database.
     * If an exception occurs (e.g., database unavailable), it disables MySQL
     * and falls back to deleting the game from the local database.
     *
     * @param saveID the identifier of the game to delete
     *
     * @author Benjamin Benyo Endahl Hansen
     */

    @Override
    public void deleteSavedGame(String saveID) {
        try {
            active().deleteSavedGame(saveID);
        } catch (Exception e) {
            disableMySQL();
            local.deleteSavedGame(saveID);
        }
    }


    /**
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @Override
    public boolean checkUserInGame(String userID, String saveID) {
        try {
            return active().checkUserInGame(userID, saveID);
        } catch (Exception e) {
            disableMySQL();
            return local.checkUserInGame(userID, saveID);
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public List<String> getSavedGames(String userID) {
        try {
            return active().getSavedGames(userID);
        } catch (Exception e) {
            disableMySQL();
            return local.getSavedGames(userID);
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public boolean wipeGameDatabase() {
        try {
            return active().wipeGameDatabase();
        } catch (Exception e) {
            disableMySQL();
            return local.wipeGameDatabase();
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */

    @Override
    public JsonNode getGameSnapshot(String saveID) {
        try {
            return active().getGameSnapshot(saveID);
        } catch (Exception e) {
            disableMySQL();
            return local.getGameSnapshot(saveID);
        }
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Override
    public JsonNode getUsers(String saveID) {
        try {
            return active().getUsers(saveID);
        } catch (Exception e) {
            disableMySQL();
            return local.getUsers(saveID);
        }
    }
}
