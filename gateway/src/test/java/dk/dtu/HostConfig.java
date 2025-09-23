package dk.dtu;

/*
Author(s): Niklas, Karl
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.model.Host;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

@TestConfiguration
public class HostConfig {

    @Bean
    @Primary
    public Host host() {
        return new Host() {
            @Override
            public UUID startGame(int amountPlayers, int boardSize) {
                return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            }

            @Override
            public void setSession(WebSocketSession session) {
            }

            @Override
            public void handleMessage(ObjectNode msg) {
            }
        };
    }
}