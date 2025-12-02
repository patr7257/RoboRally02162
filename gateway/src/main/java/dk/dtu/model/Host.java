package dk.dtu.model;

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
import java.util.concurrent.Executor;

/**
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 * @author Bjarke Søderhamn Petersen
 */

@Component
public class Host { //TODO: maybe make singleton

    private WebSocketSession session;
    private MessageQueue queue;
    private String hostURL = "http://localhost:2948/";

    public Host(){};

    //test constructor:

    /**
     * @author Niklas Emil Lysdal
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public void setSession(WebSocketSession session) {
        this.session = session;
        this.queue = new MessageQueue(session);
    }

    //test function
    public void testSetSession(WebSocketSession session, Executor executor) {
        this.session = session;
        // Pass the executor down to the queue
        this.queue = new MessageQueue(session, executor);
    }

    /**
     * @author Karl Johannes Agerbo
     */
    public void handleMessage(ObjectNode msg) {
        queue.enqueue(msg);

    }
    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
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
     * Starts a game with a board template.
     * @author Patrick Røbel
     */
    public UUID startGameWithTemplate(int amountPlayers, JsonNode boardTemplate) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = Map.of(
                "amountPlayers", amountPlayers,
                "boardTemplate", boardTemplate
        );

        String response = restTemplate.postForObject(
                hostURL + "startGameWithTemplate",
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
