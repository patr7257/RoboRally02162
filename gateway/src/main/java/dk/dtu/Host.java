package dk.dtu;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;

@Component
public class Host { //TODO: maybe make singleton

    private  WebSocketSession session;

    public Host() {

    }

    public void setSession(WebSocketSession session) {
        this.session=session;
    }

    public void handleMessage(ObjectNode msg) {
        try {
            session.sendMessage(new TextMessage(JsonUtil.toJson(msg)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID startGame() {
        return UUID.randomUUID(); //TODO: Make API call
    }
}
