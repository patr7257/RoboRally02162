package dk.dtu;

/*
Author(s): Niklas, Karl
 */

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class HostConfig {
    @Bean
    public Host host() {
        Host mockHost = mock(Host.class);
        when(mockHost.startGame()).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        return mockHost;
    }

}
