package dk.dtu.infrastructure.websocket;


import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket gateway client that manages connection to the gateway.
 *
 * @author Weihao Mo
 */
@Component
public class GatewayClient {
    private final WebSocketClient client;
    private final GatewaysWsHandler handler;

    /**
     * @author Weihao Mo
     */
    public GatewayClient(GatewaysWsHandler handler, WebSocketClient client) {
        this.client = client;
        this.handler = handler;
        this.connect("ws://localhost:8080/host");
    }

    /**
     * @author Weihao Mo
     */
    public CompletableFuture<WebSocketSession> connect(String gatewayUrl) {
        URI uri = URI.create(gatewayUrl);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        return client.execute(handler, headers, uri);
    }

}
