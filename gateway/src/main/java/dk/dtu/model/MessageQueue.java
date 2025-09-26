package dk.dtu.model;

/*
Author(s): Karl
*/

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

public class MessageQueue {
    private final BlockingQueue<ObjectNode> queue = new LinkedBlockingQueue<>();
    private final WebSocketSession session;
    //private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public MessageQueue(WebSocketSession session) {
        this.session = session;
    }

    public void enqueue(ObjectNode msg) {
        queue.add(msg);
    }

    public void flush() {
        while (!queue.isEmpty()) {
            ObjectNode msg = queue.peek();
            try {
                session.sendMessage(new TextMessage(JsonUtil.toJson(msg)));
                queue.poll();
            } catch (IOException e) {
                throw new RuntimeException(e);
                //scheduler.schedule(this::flush, 500, TimeUnit.MILLISECONDS); //TODO: check this
                //break;
            }
        }
    }
}
