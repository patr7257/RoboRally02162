package dk.dtu.model;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.util.JsonUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class Host { //TODO: maybe make singleton

    private WebSocketSession session;
    private MessageQueue queue;
    private String hostURL = "http://localhost:2948/";

    public Host(){};
    /**
     * @author Niklas Emil Lysdal
     */
    public void setSession(WebSocketSession session) {
        this.session = session;
        this.queue = new MessageQueue(session);
    }
    /**
     * @author Niklas Emil Lysdal
     */
    public void handleMessage(ObjectNode msg) {
        queue.enqueue(msg);
        queue.flush();
    }
    /**
     * @author Niklas Emil Lysdal
     */
    public UUID startGame(int amountPlayers, int boardSize) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Integer> body = Map.of(
                "amountPlayers", amountPlayers,
                "boardSize", boardSize
        );

        String response = restTemplate.postForObject(
                hostURL + "startGame", // TODO: agree on port
                body,
                String.class
        );

        String gameID = JsonUtil.parser(response).get("gameID").asText();

        return UUID.fromString(gameID);
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    public UUID startLoadedGame(int amountPlayers, int boardSize, JsonNode gameInfo) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = Map.of(
                "amountPlayers", amountPlayers,
                "boardSize", boardSize,
                "gameInfo", gameInfo
        );

        String response = restTemplate.postForObject(
                hostURL + "startLoadedGame", // TODO: agree on port
                body,
                String.class
        );

        String gameID = JsonUtil.parser(response).get("gameID").asText();

        return UUID.fromString(gameID);
    }
    /**
     * @author Niklas Emil Lysdal
     */
    public void endGame(UUID gameID) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = Map.of(
                "gameID",gameID.toString()
        );

        String response = restTemplate.postForObject(
                hostURL + "endGame",
                body,
                String.class
        );

    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    public JsonNode saveGame(UUID gameID) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = Map.of("gameID", gameID.toString());

        String response = restTemplate.postForObject(
                hostURL + "saveGame",
                body,
                String.class
        );

        return JsonUtil.parser(response);
    }
}
