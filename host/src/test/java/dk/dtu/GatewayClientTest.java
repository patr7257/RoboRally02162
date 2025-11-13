package dk.dtu;

import dk.dtu.domain.core.GameManager;
import dk.dtu.infrastructure.websocket.GatewayClient;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Weihao Mo
 */
public class GatewayClientTest {

    private WebSocketClient mockClient;
    private GatewaysWsHandler handler;
    private GatewayClient gatewayClient;
    private WebSocketSession mockSession;

    /**
     * @author Weihao Mo
     */
    @BeforeEach
    void setup() {
        mockClient = mock(WebSocketClient.class);
        handler = new GatewaysWsHandler(mock(GameManager.class));
        gatewayClient = new GatewayClient(handler, mockClient);
        mockSession = mock(WebSocketSession.class);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void testConnectReturnsSession() {
        CompletableFuture<WebSocketSession> future = CompletableFuture.completedFuture(mockSession);
        when(mockClient.execute(eq(handler), any(WebSocketHttpHeaders.class), any(URI.class))).thenReturn(future);

        CompletableFuture<WebSocketSession> resultFuture = gatewayClient.connect("ws://localhost:8080/host");
        WebSocketSession result = resultFuture.join();

        assertEquals(mockSession, result);
        (mockClient).execute(eq(handler), any(WebSocketHttpHeaders.class), any(URI.class));
    }

}
