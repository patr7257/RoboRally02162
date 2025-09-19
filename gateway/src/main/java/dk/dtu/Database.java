package dk.dtu;

/*
Author(s): Niklas
 */

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Database implements UserDatabase {
    // id -> user (all user data)
    private final Map<String,User> usersById = new HashMap<>(); //TODO: change to be ID based.
    // name -> id (secondary index giving us user id from name)
    private final Map<String, String> idByName  = new ConcurrentHashMap<>();

    //public Database() {};

    @Override
    public User createUser(String name, String passwordHash) { //TODO: add UUID generation.
        if (existsName(name)) throw new IllegalArgumentException("User exists");
        String id = UUID.randomUUID().toString();
        User user = new User(id, name, passwordHash);
        usersById.put(id, user);
        idByName.put(name,id);
        return user;
    }


    @Override
    public User findUserById(String id) { //TODO: change to actually use ID. Since the map is name based currently
        /* User user = usersById.get(name);
        if (user == null) {
            throw new NoSuchElementException("User with ID " + name + " not found");
        }
        return user; */
        return usersById.get(id);
    }

    @Override
    public boolean existsID(String id) {
        return usersById.containsKey(id);
    }

    @Override
    public boolean existsName(String name) { //TODO: remove as should be ID based
        return  idByName.containsKey(name);
    }

    @Override
    public User findUserByName(String name) {
        /* User user = usersById.get(name);
        if (user == null) {
            throw new NoSuchElementException("User with ID " + name + " not found");
        }
        return user; */
        String id = idByName.get(name);
        if (id == null) return null;
        return usersById.get(id);
    }

    @Override
    public boolean deleteUser(String id) {
        if (usersById.containsKey(id)) {usersById.remove(id); return true;}

        return false; //user doesnt exist.
    }

}
