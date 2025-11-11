package dk.dtu.model.database;

/*
Author(s): Asger
*/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class SQLDatabaseInitializer {

    //Credentials
    private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String DBURL = "jdbc:mysql://localhost:3306/RoboRallyDatabase";
    private static final String USER = "RoboRallyUser";
    private static final String PASSWORD = "RoboRallyDatabaseUser";

    public static void initializeDatabaseComplete() {
        createDatabase();
        createTables();
    }

    public static void createDatabase() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS RoboRallyDatabase");
            System.out.println("Database ready (created or already exists)");

        } catch (SQLException e) {
            System.out.println("Error when creating database");
        }
    }

    public static void createTables() {

        try (Connection conn = DriverManager.getConnection(DBURL, USER, PASSWORD);
                Statement stmt = conn.createStatement()) {

            String createUserTable = """
                        CREATE TABLE IF NOT EXISTS users (
                            UserID CHAR(36) PRIMARY KEY,
                            UserName VARCHAR(50) UNIQUE NOT NULL,
                            NickName VARCHAR(50),
                            HashedPassword VARCHAR(255) NOT NULL,
                            createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """;

            String createGamesTable = """
                        CREATE TABLE IF NOT EXISTS games (
                            SaveID CHAR(36) PRIMARY KEY,
                            Snapshot JSON NOT NULL
                        )
                    """;

            String createSavesTable = """
                        CREATE TABLE IF NOT EXISTS saves (
                            UserID CHAR(36),
                            SaveID CHAR(36),
                            PRIMARY KEY (UserID, SaveID),
                            FOREIGN KEY (UserID) REFERENCES users(UserID),
                            FOREIGN KEY (SaveID) REFERENCES games(SaveID)
                        )
                    """;

            stmt.executeUpdate(createUserTable);
            stmt.executeUpdate(createGamesTable);
            stmt.executeUpdate(createSavesTable);

            System.out.println("Tables created successfully");

        } catch (SQLException e) {
            System.out.println("Error when creating tables");
        }
    }

    public static void main(String[] args) {
        createDatabase();
        createTables();
    }
}