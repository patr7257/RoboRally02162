package dk.dtu.model;

/*
Author(s): Niklas
 */

import dk.dtu.interfaces.UserDatabase;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service("localDatabase")
public class Database implements UserDatabase {
    // id -> user (all user data)
    private final Map<String, User> usersById = new ConcurrentHashMap<>(); //TODO: change to be ID based.
    // name -> id (secondary index giving us user id from name)
    private final Map<String, String> idByName  = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    //public Database() {};

    @Override
    public User createUser(String name, String passwordHash) {
        if (existsName(name)) throw new IllegalArgumentException("User exists");
        String id = UUID.randomUUID().toString();
        User user = new User(id, name, passwordHash);
        usersById.put(id, user);
        idByName.put(name,id);
        return user;
    }


    @Override
    public User findUserById(String id) {
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
    public User findUserByNamePassword(String name, String passwordHash){
        //filter users
        List<User> result = usersById.values().stream().filter(user -> user.getName().equals(name) && encoder.matches(passwordHash,user.getPasswordHash())).collect(Collectors.toList());
        if (result.isEmpty()) return null;
        return result.get(0);

    }

    @Override
    public boolean deleteUser(String id) {
        if (usersById.containsKey(id)) {usersById.remove(id); return true;}

        return false; //user doesnt exist.
    }

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


    public boolean existsNamePassword(String name, String passwordHash) {
        return usersById.values().stream().anyMatch(u -> u.getName().equals(name) && encoder.matches(passwordHash,u.getPasswordHash()));
    }

}
