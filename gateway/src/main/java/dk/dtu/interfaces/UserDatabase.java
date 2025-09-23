package dk.dtu.interfaces;

/*
Author(s): Niklas
 */


import dk.dtu.model.User;

public interface UserDatabase {
    public User createUser(String name, String passwordHash);
    public User findUserById(String id);
    public boolean existsID (String id);
    public boolean existsName (String name);
    public User findUserByName(String name);
    public boolean deleteUser(String id);
    public boolean wipeUserDatabase(); //Resets entire user database
}

