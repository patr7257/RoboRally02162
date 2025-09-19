package dk.dtu;

/*
Author(s): Niklas
 */


public interface UserDatabase {
    public User createUser(String name, String passwordHash);
    public User findUserById(String id);
    public boolean existsID (String id);
    public boolean existsName (String name);
    public User findUserByName(String name);
    public boolean deleteUser(String id);
}

