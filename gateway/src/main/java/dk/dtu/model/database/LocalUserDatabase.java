package dk.dtu.model.database;

import dk.dtu.dto.ChangeUserNameResponse;
import dk.dtu.interfaces.UserDatabase;
import dk.dtu.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Niklas Emil Lysdal
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Kajsa Alice Ulrika Berlstedt
 * @author Weihao Mo
 * @author Karl Johannes Agerbo
 */

@Service("localUserDatabase")
public class LocalUserDatabase implements UserDatabase {
    // id -> user (all user data)
    private final Map<String, User> usersById = new ConcurrentHashMap<>(); //TODO: change to be ID based.
    // name -> id (secondary index giving us user id from name)
    private final Map<String, String> idByName = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    //public Database() {};

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    @Override
    public User createUser(String name, String passwordHash) {
        if (existsName(name)) throw new IllegalArgumentException("User exists");
        String id = UUID.randomUUID().toString();
        User user = new User(id, name, passwordHash);
        usersById.put(id, user);
        idByName.put(name, id);
        return user;
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    @Override
    public User findUserById(String id) {
        return usersById.get(id);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    @Override
    public boolean existsID(String id) {
        return usersById.containsKey(id);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    @Override
    public boolean existsName(String name) { //TODO: remove as should be ID based
        return idByName.containsKey(name);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    @Override
    public User findUserByName(String name) {
        String id = idByName.get(name);
        if (id == null) return null;
        return usersById.get(id);
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    @Override
    public User findUserByNamePassword(String name, String passwordHash) {
        //filter users
        List<User> result = usersById.values().stream().filter(user -> user.getName().equals(name) && encoder.matches(passwordHash, user.getPasswordHash())).collect(Collectors.toList());
        if (result.isEmpty()) return null;
        return result.get(0);

    }

    /**
     * @author Niklas Emil Lysdal
     * @author Weihao Mo
     * @author Karl Johannes Agerbo
     */
    @Override
    public boolean deleteUser(String id) {
        if (usersById.containsKey(id)) {
            User user = usersById.get(id);
            String userName = user.getName();
            usersById.remove(id);
            idByName.remove(userName);
            return true;
        }
        return false; //user doesnt exist.
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Override
    public synchronized boolean wipeUserDatabase() { //clears user database
        try {
            this.usersById.clear();
            this.idByName.clear();
            //this might need a backup in case one fails and the other doesn't, such that it can be rolled back.
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * @author Niklas Emil Lysdal
     */

    public boolean existsNamePassword(String name, String passwordHash) {
        return usersById.values().stream().anyMatch(u -> u.getName().equals(name) && encoder.matches(passwordHash, u.getPasswordHash()));
    }

    /**
     * @return
     * @return true if change was successful/allowed
     * @author: Niklas Emil Lysdal
     */
    @Override
    public synchronized ChangeUserNameResponse changeUsername(String userID, String newName) {
        if (idByName.containsKey(newName)) {
            String id = idByName.get(newName);
            if (id.equals(userID)) {
                return ChangeUserNameResponse.SUCCESS;
            }
            return ChangeUserNameResponse.USERNAME_ALREADY_EXISTS;
        }
        if (!existsID(userID)) {
            return ChangeUserNameResponse.NO_SUCH_USER;
        }
        User user = usersById.get(userID);
        String oldName = user.getName();
        idByName.remove(oldName);
        user.setName(newName);
        idByName.put(newName, userID);
        return ChangeUserNameResponse.SUCCESS;
    }

}
