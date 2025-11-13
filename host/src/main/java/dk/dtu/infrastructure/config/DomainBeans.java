package dk.dtu.infrastructure.config;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.core.GameScheduler;
import dk.dtu.domain.core.RoundPacer;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
@Configuration
public class DomainBeans {

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    @Bean(destroyMethod = "shutdown")
    public RoundPacer roundPacer() {
        return new GameScheduler();
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    @Bean
    public GameManager gameManager(RoundPacer roundPacer) {
        return new GameManager(roundPacer);
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    @Bean
    public GatewaysWsHandler gatewaysWsHandler(GameManager gameManager) {
        return new GatewaysWsHandler(gameManager);
    }
}
