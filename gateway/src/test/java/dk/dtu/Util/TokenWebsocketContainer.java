package dk.dtu.Util;

import org.springframework.web.socket.WebSocketSession;

/**
 * @author Niklas Emil Lysdal
 */
public class TokenWebsocketContainer {
    private WebSocketSession session;
    private String userIDToken;
    /**
     * @author Niklas Emil Lysdal
     */
    public TokenWebsocketContainer(WebSocketSession session, String userIDToken) {
        this.session = session;
        this.userIDToken = userIDToken;
    }

    public String getUserIDToken() {return this.userIDToken;}
    public WebSocketSession getSession() {return this.session;}
}
