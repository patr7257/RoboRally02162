package dk.dtu;

/*
Author(s): Karl
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public class Client {
    private final String sessionID;
    private final User user;
    private final WebSocketSession session;

    public Client(String sessionId, User user, WebSocketSession session) {
        this.sessionID = sessionId;
        this.user = user;
        this.session = session;
    }

    public void handleMessage(ObjectNode msg) {
        try {
            String m = JsonUtil.toJson(msg); 
            System.out.println("sending:"+m);
            User user = (User) session.getAttributes().get("user");
            String userID = user.getUserID();
            System.out.println(userID);
            session.sendMessage(new TextMessage(JsonUtil.toJson(msg)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getUsername() {
        return this.user.getName();
    }

    public String getUserID() {
        return this.user.getUserID();
    }

    public WebSocketSession getSession() {
        return session;
    }
}
