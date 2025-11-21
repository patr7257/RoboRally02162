package dk.dtu.Util;

import org.springframework.web.socket.WebSocketSession;

/**
 * @author Niklas Emil Lysdal
 */
public class TokenWebsocketContainer {
    private WebSocketSession session;
    private String userToken;

    /**
     * @author Niklas Emil Lysdal
     */
    public TokenWebsocketContainer(WebSocketSession session, String userToken) {
        this.session = session;
        this.userToken = userToken;
    }

    public String getUserToken() {return this.userToken;}
    public WebSocketSession getSession() {return this.session;}
}
