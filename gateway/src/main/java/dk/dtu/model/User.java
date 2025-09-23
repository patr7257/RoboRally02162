package dk.dtu.model;

/*
Author(s): Niklas
 */

public class User {

    private String name;
    private String userID;
    private String passwordHash; // the BCrypt of client SHA-256

    public User(String userID, String name, String passwordHash) {
        this.name = name;
        this.userID = userID; //TODO: change to be a generated userID.
        this.passwordHash = passwordHash;
    }
    public String getName() {return this.name;}
    public String getUserID() {return this.userID;}
    public String getPasswordHash() { return passwordHash; }
}
