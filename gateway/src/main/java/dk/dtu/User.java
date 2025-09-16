package dk.dtu;

/*
Author(s): Niklas
 */

public class User {

    private String name;
    private String userID;
    public User(String name) {
        this.name = name;
        this.userID = name; //TODO: change to be a generated userID.
    }
    public String getName() {return this.name;}
    public String getUserID() {return this.userID;}
}
