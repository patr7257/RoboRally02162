package dk.dtu.model;

/*
Author(s): Karl
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public class Client {
    private final String sessionID;
    private final User user;
    private final WebSocketSession session;
    private final MessageQueue queue;
    /**
     * @author Niklas Emil Lysdal
     */
    public Client(User user, WebSocketSession session) {
        this.sessionID = session.getId();
        this.user = user;
        this.session = session;
        this.queue = new MessageQueue(session);
    }

    public void handleMessage(ObjectNode msg) {
        queue.enqueue(msg);
        queue.flush();
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
