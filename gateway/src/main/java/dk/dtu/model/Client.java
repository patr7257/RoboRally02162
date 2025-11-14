package dk.dtu.model;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
/**
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */
public class Client {
    private final String sessionID;
    private final User user;
    private final WebSocketSession session;
    private final MessageQueue queue;
    
    /**
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    public Client(User user, WebSocketSession session) {
        this.sessionID = session.getId();
        this.user = user;
        this.session = session;
        this.queue = new MessageQueue(session);
    }
    
    /**
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */
    public void handleMessage(ObjectNode msg) {
        if (!isSessionOpen()) {
            return;
        }
        queue.enqueue(msg);
        queue.flush();
    }
    
    /**
     * @author Asger Allin Jensen
     */
    public boolean isSessionOpen() {
        return session != null && session.isOpen();
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