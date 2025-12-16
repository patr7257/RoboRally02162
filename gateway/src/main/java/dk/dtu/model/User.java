package dk.dtu.model;

/**
 * @author Niklas Emil Lysdal
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Kajsa Alice Ulrika Berlstedt
 */

public class User {

    private String name;
    private String userID;
    private String passwordHash; // the BCrypt of client SHA-256

    /**
     * @author Niklas Emil Lysdal
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    public User(String userID, String name, String passwordHash) {
        this.name = name;
        this.userID = userID;
        this.passwordHash = passwordHash;
    }

    public String getName() {return this.name;}
    public String getUserID() {return this.userID;}
    public String getPasswordHash() { return passwordHash; }
    public void setName(String name) {this.name = name;}
}
