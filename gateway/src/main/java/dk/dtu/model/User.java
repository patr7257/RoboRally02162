package dk.dtu.model;



public class User {

    private String name;
    private String userID;
    private String passwordHash; // the BCrypt of client SHA-256
    /**
     * @author Niklas Emil Lysdal
     */
    public User(String userID, String name, String passwordHash) {
        this.name = name;
        this.userID = userID; //TODO: change to be a generated userID.
        this.passwordHash = passwordHash;
    }
    public String getName() {return this.name;}
    public String getUserID() {return this.userID;}
    public String getPasswordHash() { return passwordHash; }
}
