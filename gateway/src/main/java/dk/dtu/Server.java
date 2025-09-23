package dk.dtu;

/*
Author(s): Niklas, Karl, Benjamin
 */

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.User;
import dk.dtu.shared.ServerRegistry;
import dk.dtu.util.JsonUtil;
import dk.dtu.config.ClientHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@SpringBootApplication
@EnableWebSocket
public class Server implements WebSocketConfigurer,CommandLineRunner { // TODO: after host connects remove

    private final ClientHandshakeInterceptor clientInterceptor;
    private final ServerRegistry serverRegistry;
    @Autowired
    public Server(ClientHandshakeInterceptor cliHandInt, ServerRegistry serverRegistry) {
        this.clientInterceptor = cliHandInt;
        this.serverRegistry = serverRegistry;
    }

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);

    }

    private String getTokenFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
        }
        return "unknown";
    }

    private String getUserFromSession(WebSocketSession session) {
        return getTokenFromSession(session);
    }

    // TODO: remove this (run) method after host starts connecting to gateway
    // instead of the other way.
    @Override
    public void run(String... args) throws Exception {
        StandardWebSocketClient targetClient = new StandardWebSocketClient();
        String hostUrl = "ws://localhost:2948/ws"; // change to your host's URL

        try {
            WebSocketSession session = targetClient.doHandshake(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    String jSonText = message.getPayload();
                    System.out.println(jSonText);
                    JsonNode json = JsonUtil.parser(jSonText);
                    String gameID = json.get("meta").get("game").get("gameID").asText();

                    String lobbyID = serverRegistry.getGameToLobby().get(gameID); // TODO: check for valid ID
                    Lobby lob = serverRegistry.getLobbies().get(lobbyID);
                    lob.handleHostMessage(json);

                }
            }, hostUrl).get();

            serverRegistry.getHost().setSession(session);
            System.out.println("Connected to host!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler() { // Client
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                String token = getTokenFromSession(session);
                System.out.println("=== WebSocket CONNECTED ===");
                System.out.println("Session ID: " + session.getId());
                System.out.println("User: " + token);
                System.out.println("Session state: " + session.isOpen());
                System.out.println("========================");
                User user = (User) session.getAttributes().get("user");
                Client client = new Client(session.getId(), user, session);
                serverRegistry.getClients().put(client.getUsername(), client); // TODO: change to ID
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                try {
                    String jSonText = message.getPayload().toString();
                    System.out.println(jSonText);
                    JsonNode json = JsonUtil.parser(jSonText);
                    String lobbyID = json.get("lobbyID").asText();
                    System.out.println(lobbyID);
                    User user = (User) session.getAttributes().get("user");
                    String userID = user.getUserID();
                    System.out.println(userID);
                    Lobby lob = serverRegistry.getLobbies().get(lobbyID); // TODO: check for valid ID
                    lob.handleClientMessage(userID, json.get("payload")); // TODO: Check that toString() is correct
                } catch (Exception e) {
                    System.err.println("=== ERROR IN MESSAGE HANDLING ===");
                    System.err.println("Session ID: " + session.getId());
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.err.println("================================");
                }
                System.out.println("Message handling completed for: " + session.getId());
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.err.println("=== TRANSPORT ERROR ===");
                System.err.println("Session ID: " + session.getId());
                System.err.println("User: " + getUserFromSession(session));
                System.err.println("Error: " + exception.getMessage());
                exception.printStackTrace();
                System.err.println("=====================");
            }

            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                System.err.println("=== WebSocket CLOSED ===");
                System.err.println("Session ID: " + session.getId());
                System.err.println("User: " + getUserFromSession(session));
                System.err.println("Close code: " + closeStatus.getCode());
                System.err.println("Close reason: " + closeStatus.getReason());
                System.err.println("Was clean: " + closeStatus.toString());
                System.err.println("Close triggered from:");
                Thread.dumpStack();
                System.err.println("====================");
                System.out.println("Client disconnected: " + session.getId());
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }

        }, "/client").addInterceptors(clientInterceptor).setAllowedOrigins("*");

        registry.addHandler(new WebSocketHandler() { // host
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                String token = getTokenFromSession(session);
                System.out.println("=== WebSocket CONNECTED ===");
                System.out.println("Session ID: " + session.getId());
                System.out.println("User: " + token);
                System.out.println("Session state: " + session.isOpen());
                System.out.println("========================");
                serverRegistry.getHost().setSession(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String jSonText = message.getPayload().toString();
                JsonNode json = JsonUtil.parser(jSonText);
                String gameID = json.get("meta").get("game").get("gameID").asText();

                String lobbyID = serverRegistry.getGameToLobby().get(gameID); // TODO: check for valid ID
                Lobby lob = serverRegistry.getLobbies().get(lobbyID);
                lob.handleHostMessage(json);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // TODO: handle host disconnecting (probably message all clients that server is
                // down)
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, "/host").setAllowedOrigins("*");
    }

    public Map<String, Lobby> getLobbies() {
        return serverRegistry.getLobbies();
    }

    public Map<String, String> getGameToLobby() {
        return serverRegistry.getGameToLobby();
    }

    public Map<String, Client> getClients() {
        return serverRegistry.getClients();
    }


}
