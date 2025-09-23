package dk.dtu.infrastructure.websocket;


import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

// Author(s) Weihao Mo

@Component
public class GatewayClient {
    private final WebSocketClient client;
    private final GatewaysWsHandler handler;

    public GatewayClient(GatewaysWsHandler handler, WebSocketClient client) {
        this.client = client;
        this.handler = handler;
        this.connect("ws://localhost:8080/host");
    }

    public CompletableFuture<WebSocketSession> connect(String gatewayUrl) {
        URI uri = URI.create(gatewayUrl);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        return client.execute(handler, headers, uri);
    }

}
