package dk.dtu.infrastructure.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

// Author(s) William Pii Jæger

@Configuration
public class WebSocketConfig {
    @Bean
    public WebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }
}
