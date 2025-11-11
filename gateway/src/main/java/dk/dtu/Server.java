package dk.dtu;

/*
Author(s): Niklas, Karl, Benjamin
 */

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.model.database.SQLDatabaseInitializer;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.User;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;
import dk.dtu.config.ClientHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;

import java.util.*;

@SpringBootApplication
@EnableWebSocket
public class Server implements WebSocketConfigurer { // TODO: after host connects remove

    private final ClientHandshakeInterceptor clientInterceptor;
    private final ServerManager serverManager;
    private WebSocketHandler clientHandler;
    private WebSocketHandler hostHandler;

    @Autowired
    public Server(ClientHandshakeInterceptor cliHandInt, ServerManager serverManager) {
        SQLDatabaseInitializer.initializeDatabaseComplete();
        this.clientInterceptor = cliHandInt;
        this.serverManager = serverManager;
        initClientHandler();
        initHostHandler();
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientHandler, "/client").addInterceptors(clientInterceptor).setAllowedOrigins("*");

        registry.addHandler(hostHandler, "/host").setAllowedOrigins("*");
    }


    private void initClientHandler() {
        clientHandler = new WebSocketHandler() { // Client
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {

                String token = getTokenFromSession(session);
                User user = (User) session.getAttributes().get("user");
                System.out.println("=== WebSocket CONNECTED ===");
                System.out.println("Session ID: " + session.getId());
                System.out.println("User: " + user.getName());
                System.out.println("Session state: " + session.isOpen());
                System.out.println("========================");

                serverManager.createClient(user,session);
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
                    //System.out.println(userID);
                    Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID); // TODO: check for valid ID
                    lob.handleClientMessage(userID, json); // TODO: Check that toString() is correct
                } catch (Exception e) {
                    System.err.println("=== ERROR IN MESSAGE HANDLING ===");
                    System.err.println("Session ID: " + session.getId());
                    System.err.println("Error: " + e.getMessage());
                    //e.printStackTrace();
                    System.err.println("================================");
                }
                //System.out.println("Message handling completed for: " + session.getId());
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.err.println("=== TRANSPORT ERROR ===");
                System.err.println("Session ID: " + session.getId());
                System.err.println("User: " + getUserFromSession(session));
                System.err.println("Error: " + exception.getMessage());
                //exception.printStackTrace();
                System.err.println("=====================");
            }

            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                System.err.println("=== WebSocket CLOSED ===");
                System.err.println("Session ID: " + session.getId());
                System.err.println("User: " + getUserFromSession(session));
                System.err.println("Close code: " + closeStatus.getCode());
                System.err.println("Close reason: " + closeStatus.getReason());
                System.err.println("Was clean: " + closeStatus);
                System.err.println("Close triggered from:");
                Thread.dumpStack();
                System.err.println("====================");
                System.out.println("Client disconnected: " + session.getId());
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }

        };
    }

    private void initHostHandler() {
        hostHandler = new WebSocketHandler() { // host
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                String token = getTokenFromSession(session);
                System.out.println("=== WebSocket CONNECTED ===");
                System.out.println("Session ID: " + session.getId());
                System.out.println("Host");
                System.out.println("Session state: " + session.isOpen());
                System.out.println("========================");
                serverManager.getHost().setSession(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                try {
                    String jSonText = message.getPayload().toString();
                    JsonNode json = JsonUtil.parser(jSonText);

                    JsonNode meta = json.path("meta").path("game");
                    String gameID = meta.path("gameID").asText(null);
                    if (gameID == null || gameID.isBlank()) {
                        System.err.println("[HOST] Missing gameID in message: " + jSonText);
                        return;
                    }

                    String lobbyID = serverManager.getLobbyIDFromGameID(gameID);

                    if (lobbyID == null) {
                        System.err.println("[HOST] Unknown gameID " + gameID + " — no lobby mapping yet");
                        return;
                    }

                    Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
                    if (lob == null) {
                        System.err.println("[HOST] Stale mapping: lobby " + lobbyID + " not found for game " + gameID + ". Cleaning up.");
                        serverManager.removeGameMapping(gameID);
                        return;
                    }

                    lob.handleHostMessage(json);
                } catch (Exception e) {
                    System.err.println("[HOST] handleMessage error: " + e.getMessage());
                   // e.printStackTrace();
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // TODO: handle host disconnecting (probably message all clients that server is down)
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };
    }

    public Map<String, Lobby> getLobbiesForTest() {
        return serverManager.getLobbiesForTest();
    }

    public Map<String, String> getGameToLobbyForTest() {
        return serverManager.getGameToLobbyForTest();
    }

    public Map<String, Client> getClientsForTest() {
        return serverManager.getClientsForTest();
    }

    public WebSocketHandler getClientHandler() {
        return clientHandler;
    }

    public WebSocketHandler getHostHandler() {
        return hostHandler;
    }



}
