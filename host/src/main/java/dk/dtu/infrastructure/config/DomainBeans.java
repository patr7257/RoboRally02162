package dk.dtu.infrastructure.config;

import dk.dtu.domain.core.GameManager;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Author(s) Weihao Mo, William Pii Jæger

@Configuration
public class DomainBeans {
    @Bean
    public GameManager gameManager() {
        return new GameManager();
    }

    @Bean
    public GatewaysWsHandler gatewaysWsHandlerNew() {
        return new GatewaysWsHandler(gameManager());
    }
}