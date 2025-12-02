package dk.dtu.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Karl Johannes Agerbo
 */

public class MessageQueue {
    private final BlockingQueue<ObjectNode> queue = new LinkedBlockingQueue<>();
    private  WebSocketSession session;
    private final Object lock = new Object();
    private final Executor executor;
    /**
     * @author Karl Johannes Agerbo
     */

    public MessageQueue(WebSocketSession session) {
        this(session,Executors.newSingleThreadExecutor());
    }
    public MessageQueue(WebSocketSession session, Executor executor) {
        this.session = session;
        this.executor = executor;
    }
    /**
     * @author Karl Johannes Agerbo
     */

    public void enqueue(ObjectNode msg) {
        queue.add(msg);
        executor.execute(this::flush);
    }

    /**
     * @author Karl Johannes Agerbo
     */

    public void flush() {
        synchronized (lock) {
            while (!queue.isEmpty()) {
                if (!session.isOpen()) {
                    return;
                }
                ObjectNode msg = queue.peek();
                try {
                    session.sendMessage(new TextMessage(JsonUtil.toJson(msg)));
                    queue.poll();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public synchronized void replaceSession(WebSocketSession session) {
        synchronized (lock) {
            this.session = session;
        }
        flush();
    }
}
