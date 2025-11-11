package dk.dtu.model.database;

/*
Author(s): Asger
*/

import dk.dtu.interfaces.UserDatabase;
import dk.dtu.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class DynamicUserDatabase implements UserDatabase {

    private final UserDatabase mysql;
    private final UserDatabase local;
    private boolean mysqlAvailable = true;

    public DynamicUserDatabase(@Qualifier("mysqlUserDatabase") UserDatabase mysql,
            @Qualifier("localUserDatabase") UserDatabase local) {
        this.mysql = mysql;
        this.local = local;
        this.mysqlAvailable = checkMySQLAvailable();
        if (!mysqlAvailable)
            System.err.println("MySQLUserDatabase not available, using local fallback database.");
    }

    private boolean checkMySQLAvailable() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/RoboRallyDatabase",
                "RoboRallyUser", "RoboRallyDatabaseUser")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private UserDatabase active() {
        return mysqlAvailable ? mysql : local;
    }

    private void disableMySQL() {
        System.err.println("Switching to local fallback database due to MySQL error.");
        mysqlAvailable = false;
    }

    @Override
    public User createUser(String name, String passwordHash) {
        try {
            return active().createUser(name, passwordHash);
        } catch (Exception e) {
            disableMySQL();
            return local.createUser(name, passwordHash);
        }
    }

    @Override
    public User findUserById(String id) {
        try {
            return active().findUserById(id);
        } catch (Exception e) {
            disableMySQL();
            return local.findUserById(id);
        }
    }

    @Override
    public boolean existsID(String id) {
        try {
            return active().existsID(id);
        } catch (Exception e) {
            disableMySQL();
            return local.existsID(id);
        }
    }

    @Override
    public boolean existsName(String name) {
        try {
            return active().existsName(name);
        } catch (Exception e) {
            disableMySQL();
            return local.existsName(name);
        }
    }

    @Override
    public User findUserByName(String name) {
        try {
            return active().findUserByName(name);
        } catch (Exception e) {
            disableMySQL();
            return local.findUserByName(name);
        }
    }

    @Override
    public User findUserByNamePassword(String name, String passwordHash) {
        try {
            return active().findUserByNamePassword(name, passwordHash);
        } catch (Exception e) {
            disableMySQL();
            return local.findUserByNamePassword(name, passwordHash);
        }
    }

    @Override
    public boolean deleteUser(String id) {
        try {
            return active().deleteUser(id);
        } catch (Exception e) {
            disableMySQL();
            return local.deleteUser(id);
        }
    }

    @Override
    public boolean wipeUserDatabase() {
        try {
            return active().wipeUserDatabase();
        } catch (Exception e) {
            disableMySQL();
            return local.wipeUserDatabase();
        }
    }

    @Override
    public boolean existsNamePassword(String name, String passwordHash) {
        try {
            return active().existsNamePassword(name, passwordHash);
        } catch (Exception e) {
            disableMySQL();
            return local.existsNamePassword(name, passwordHash);
        }
    }
}
