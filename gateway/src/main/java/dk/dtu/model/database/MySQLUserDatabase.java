package dk.dtu.model.database;

import dk.dtu.interfaces.UserDatabase;
import dk.dtu.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.UUID;

/**
 * @author Asger Allin Jensen
 */

@Service("mysqlUserDatabase")
public class MySQLUserDatabase implements UserDatabase {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * @author Asger Allin Jensen
     */

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DatabaseCredentials.DBURL, DatabaseCredentials.USER, DatabaseCredentials.PASSWORD);
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User createUser(String name, String passwordHash) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO users (UserID, UserName, HashedPassword) VALUES (?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, passwordHash);
            stmt.executeUpdate();
            return new User(id, name, passwordHash);
        } catch (SQLException e) {
            throw new RuntimeException("MySQL createUser failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User findUserById(String id) {
        String sql = "SELECT * FROM users WHERE UserID=?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(id, rs.getString("UserName"), rs.getString("HashedPassword"));
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("MySQL findUserById failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean existsID(String id) {
        String sql = "SELECT 1 FROM users WHERE UserID=?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("MySQL existsID failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean existsName(String name) {
        String sql = "SELECT 1 FROM users WHERE UserName=?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("MySQL existsName failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User findUserByName(String name) {
        String sql = "SELECT * FROM users WHERE UserName=?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("UserID"), name, rs.getString("HashedPassword"));
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("MySQL findUserByName failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public User findUserByNamePassword(String name, String passwordHash) {
        User user = findUserByName(name);
        if (user != null && encoder.matches(passwordHash, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean deleteUser(String id) {
        String sql = "DELETE FROM users WHERE UserID=?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("MySQL deleteUser failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean wipeUserDatabase() {
        String sql = "DELETE FROM users";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("MySQL wipeUserDatabase failed", e);
        }
    }

    /**
     * @author Asger Allin Jensen
     */

    @Override
    public boolean existsNamePassword(String name, String passwordHash) {
        return findUserByNamePassword(name, passwordHash) != null;
    }
}
