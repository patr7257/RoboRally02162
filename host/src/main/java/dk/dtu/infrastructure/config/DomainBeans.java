package dk.dtu.infrastructure.config;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.core.GameScheduler;
import dk.dtu.domain.core.RoundPacer;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainBeans {

    @Bean(destroyMethod = "shutdown")
    public RoundPacer roundPacer() {
        return new GameScheduler();
    }

    @Bean
    public GameManager gameManager(RoundPacer roundPacer) {
        return new GameManager(roundPacer);
    }

    @Bean
    public GatewaysWsHandler gatewaysWsHandler(GameManager gameManager) {
        return new GatewaysWsHandler(gameManager);
    }
}
