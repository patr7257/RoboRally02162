package dk.dtu.model;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.dto.ClientConnectReason;
import dk.dtu.dto.ClientDisconnectReason;
import dk.dtu.dto.ClientUpdateReason;
import dk.dtu.observer.ClientObserver;
import dk.dtu.util.JsonUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */
public class Client {
    private final String sessionID;
    private final User user;
    private WebSocketSession session;
    private final MessageQueue queue;
    private final Set<ClientObserver> observers  = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> disconnectCallback; //function to call upon disconnect
    private static  ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int TIMEOUT_SECONDS = 10;


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
    //Test constructor
    public Client(User user, WebSocketSession session, MessageQueue queue) {
        this.sessionID = session.getId();
        this.user = user;
        this.session = session;
        this.queue = queue;
    }

    public static void setScheduler(ScheduledExecutorService testScheduler) {
        scheduler = testScheduler;
    }
    /**
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */
    public void handleMessage(ObjectNode msg) {
        queue.enqueue(msg);
        //queue.flush();
    }
    /**
     * @author Niklas Emil Lysdal
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
    public void addObserver(ClientObserver observer) {this.observers.add(observer);}
    public void removeObserver(ClientObserver observer) {this.observers.remove(observer);}

    public void notifyObservers(ClientUpdateReason reason) {
        for (ClientObserver observer : this.observers) {
            observer.handleClientUpdate(reason,this);
        }
    }

    public WebSocketSession getSession() {
        return session;
    }

    /**
     * @author Niklas Emil Lysdal
     */
    public synchronized void  handleDisconnect(ClientDisconnectReason reason) {

        switch (reason) {
            case LOGOUT -> notifyObservers(ClientUpdateReason.LOGOUT);
            case CONNECTION_LOSS -> startDisconnectTimer();
        }

    }
    /**
     * @author Niklas Emil Lysdal
     */
    private void startDisconnectTimer(){
        if (disconnectCallback != null && !disconnectCallback.isDone()) {return;}

        disconnectCallback = scheduler.schedule(
                ()-> {
                    try {
                        System.out.println("reconnect timed out");
                        notifyObservers(ClientUpdateReason.DISCONNECTED);
                    }catch (Throwable t){
                        System.err.println("reconnect timer failed");
                        t.printStackTrace();
                    } },
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );

    }

    /**
     * @author Niklas Emil Lysdal
     */
    public synchronized void handleConnect(WebSocketSession socket, ClientConnectReason reason ) {
        System.out.println("client handleconnect, reason: " + reason);
        if (disconnectCallback != null ) {
            boolean cancelSuccess = disconnectCallback.cancel(false); //handler is fast, so false is safer.
            if (cancelSuccess) {
                System.out.println("client timer cancelled successfully");
            }
            this.disconnectCallback = null;
        }
        this.session = socket;
        queue.replaceSession(socket);
        switch (reason) {
            case LOGIN: notifyObservers(ClientUpdateReason.RESET); break;
            case RECONNECT: notifyObservers(ClientUpdateReason.RECONNECTED); break;
        }


    }

}