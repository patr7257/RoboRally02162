package dk.dtu.dto;

public class LobbyUserRequest {
    public String lobbyID;
    public String userID;
    public LobbyUserRequest() {};

    public LobbyUserRequest(String lobbyID,String userId) {
        this.lobbyID = lobbyID;
        this.userID = userId;
    }
}
