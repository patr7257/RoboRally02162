package dk.dtu.model.database;



import dk.dtu.interfaces.UserDatabase;
import dk.dtu.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Asger Allin Jensen
 */

@Service
public class DynamicUserDatabase implements UserDatabase {

    private final UserDatabase mysql;
    private final UserDatabase local;
    private boolean mysqlAvailable = true;

    /**
     * @author Asger Allin Jensen
     */

    public DynamicUserDatabase(@Qualifier("mysqlUserDatabase") UserDatabase mysql,
            @Qualifier("localUserDatabase") UserDatabase local) {
        this.mysql = mysql;
        this.local = local;
        this.mysqlAvailable = checkMySQLAvailable();
        if (!mysqlAvailable)
            System.err.println("MySQLUserDatabase not available, using local fallback database.");
    }

    /**
     * @author Asger Allin Jensen
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
     * @author Asger Allin Jensen
     */

    private UserDatabase active() {
        return mysqlAvailable ? mysql : local;
    }

    /**
     * @author Asger Allin Jensen
     */

    private void disableMySQL() {
        System.err.println("Switching to local fallback database due to MySQL error.");
        mysqlAvailable = false;
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User createUser(String name, String passwordHash) {
        try {
            return active().createUser(name, passwordHash);
        } catch (Exception e) {
            disableMySQL();
            return local.createUser(name, passwordHash);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User findUserById(String id) {
        try {
            return active().findUserById(id);
        } catch (Exception e) {
            disableMySQL();
            return local.findUserById(id);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean existsID(String id) {
        try {
            return active().existsID(id);
        } catch (Exception e) {
            disableMySQL();
            return local.existsID(id);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean existsName(String name) {
        try {
            return active().existsName(name);
        } catch (Exception e) {
            disableMySQL();
            return local.existsName(name);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User findUserByName(String name) {
        try {
            return active().findUserByName(name);
        } catch (Exception e) {
            disableMySQL();
            return local.findUserByName(name);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User findUserByNamePassword(String name, String passwordHash) {
        try {
            return active().findUserByNamePassword(name, passwordHash);
        } catch (Exception e) {
            disableMySQL();
            return local.findUserByNamePassword(name, passwordHash);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean deleteUser(String id) {
        try {
            return active().deleteUser(id);
        } catch (Exception e) {
            disableMySQL();
            return local.deleteUser(id);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean wipeUserDatabase() {
        try {
            return active().wipeUserDatabase();
        } catch (Exception e) {
            disableMySQL();
            return local.wipeUserDatabase();
        }
    }

    /**
     * @author Asger Allin Jensen
     */

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
