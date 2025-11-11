package dk.dtu.dto;
/**
 * @author Niklas Emil Lysdal
 */
public class LobbyUserRequest {
    public String lobbyID;
    public String userID;
    public LobbyUserRequest() {};
    /**
     * @author Niklas Emil Lysdal
     */
    public LobbyUserRequest(String lobbyID,String userId) {
        this.lobbyID = lobbyID;
        this.userID = userId;
    }
}
