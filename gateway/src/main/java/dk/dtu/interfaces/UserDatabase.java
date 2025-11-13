package dk.dtu.interfaces;


import dk.dtu.model.User;
/**
 * @author Niklas Emil Lysdal
 */
public interface UserDatabase {
    public User createUser(String name, String passwordHash);
    public User findUserById(String id);
    public boolean existsID (String id);
    public boolean existsName (String name);
    public User findUserByName(String name);
    public User findUserByNamePassword(String name, String passwordHash);
    public boolean deleteUser(String id);
    public boolean wipeUserDatabase(); //Resets entire user database
    public boolean existsNamePassword(String name, String passwordHash);
}

