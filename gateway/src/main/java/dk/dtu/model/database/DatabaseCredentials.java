package dk.dtu.model.database;

/**
 * @author Benjamin Benyo Endahl Hansen
 */
public class DatabaseCredentials {
    public static final boolean useMySQLDatabase = true; // false = local database
    public static final String URL = "jdbc:mysql://localhost:3306/";
    public static final String DBURL = "jdbc:mysql://localhost:3306/RoboRallyDatabase";
    public static final String USER = "RoboRallyUser";
    public static final String PASSWORD = useMySQLDatabase ? "RoboRallyDatabaseUser" : "";
}
