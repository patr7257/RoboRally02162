package dk.dtu.model;

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
            System.err.println("Error with database: " + e.getMessage());
            e.printStackTrace();
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

            stmt.executeUpdate(createUserTable);
            System.out.println("Table created successfully");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        createDatabase();
        createTables();
    }
}