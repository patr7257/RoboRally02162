package dk.dtu;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.model.Host;
import dk.dtu.util.JsonUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

/**
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 */

@TestConfiguration
public class HostConfig {

    public static boolean forceWinner;

    @Bean
    @Primary
    public Host host() {
        return new Host() {

            /**
             * @author Karl Johannes Agerbo
             * @author Niklas Emil Lysdal
             */
            @Override
            public UUID startGame(int amountPlayers, int boardSize) {
                return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            }

            /**
             * @author Bjarke Søderhamn Petersen
             * @author Benjamin Benyo Endahl Hansen
             * @author Karl Johannes Agerbo
             */
            @Override
            public UUID startLoadedGame(int amountPlayers, int boardSize, JsonNode gameInfo) {
                return UUID.randomUUID();
            }

            @Override
            public void setSession(WebSocketSession session) {
            }

            /**
            * @author Bjarke Søderhamn Petersen
            * @author Benjamin Benyo Endahl Hansen
            * @author Karl Johannes Agerbo
            */
            @Override
            public JsonNode saveGame(UUID gameID) {
                ObjectNode gameSnapshot  = JsonUtil.createObjectNode();
                Integer winner = forceWinner ? 1 : null;
                gameSnapshot.put("winner", winner);
                return gameSnapshot;
            }

            @Override
            public void handleMessage(ObjectNode msg) {
            }

            @Override
            public UUID startDemoGame(JsonNode gameInfo) {
                return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            }

            /**
             * @author Karl Johannes Agerbo
             */
            public void toggleDemo(UUID gameID) {
            }

            /**
             * @author Karl Johannes Agerbo
             */
            public void setDemoTimings(UUID gameID) {
            }
        };
    }
}