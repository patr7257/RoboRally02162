package dk.dtu.dto;

public class UserNameUpdateEvent {
    private final String userID;
    private final String newUsername;
    public UserNameUpdateEvent(String userID, String newUsername) {
        this.userID = userID;
        this.newUsername = newUsername;

    }
    public String getUserID() {return this.userID;}
    public String getNewUsername() {return this.newUsername;}

}
