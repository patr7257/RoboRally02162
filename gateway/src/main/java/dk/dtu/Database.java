package dk.dtu;

/*
Author(s): Niklas
 */

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
@Service
public class Database implements UserDatabase {
    private final Map<String,User> users = new HashMap<>(); //TODO: change to be ID based.
    public Database() {};

    @Override
    public User createUser(String Name) { //TODO: add UUID generation.
      User user = new User(Name);
      users.put(Name, user);
      return user;
    }



    @Override
    public User findUserById(String id) { //TODO: change to actually use ID. Since the map is name based currently
        User user = users.get(id);
        if (user == null) {
            throw new NoSuchElementException("User with ID " + id + " not found");
        }
        return user;
    }

    @Override
    public boolean existsID(String id) {
        return users.containsKey(id);
    }

    @Override
    public boolean existsName(String name) { //TODO: remove as should be ID based
        return  users.containsKey(name);
    }



    @Override
    public User findUserByName(String name) {
        User user = users.get(name);
        if (user == null) {
            throw new NoSuchElementException("User with ID " + name + " not found");
        }
        return user;
    }

    @Override
    public boolean deleteUser(String id) {
        if (users.containsKey(id)) {users.remove(id); return true;}

        return false; //user doesnt exist.
    }


}
