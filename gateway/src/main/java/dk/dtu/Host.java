package dk.dtu;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
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

    public UUID startGame(int amountPlayers, int boardSize) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Integer> body = Map.of(
                "amountPlayers", amountPlayers,
                "boardSize", boardSize
        );

        String response = restTemplate.postForObject(
                "http://localhost:2948/startgame", // TODO: agree on port
                body,
                String.class
        );

        String gameID = JsonUtil.parser(response).get("gameID").asText();

        return UUID.fromString(gameID);
    }
}
