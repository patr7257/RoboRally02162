package dk.dtu;


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

/**
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 * @author Niklas Emil Lysdal
 */
@SpringBootApplication
@EnableWebSocket
public class Server implements WebSocketConfigurer { // TODO: after host connects remove

    private final ClientHandshakeInterceptor clientInterceptor;
    private final ServerManager serverManager;
    private WebSocketHandler clientHandler;
    private WebSocketHandler hostHandler;

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Niklas Emil Lysdal
     */
    @Autowired
    public Server(ClientHandshakeInterceptor cliHandInt, ServerManager serverManager) {
        SQLDatabaseInitializer.initializeDatabaseComplete();
        this.clientInterceptor = cliHandInt;
        this.serverManager = serverManager;
        initClientHandler();
        initHostHandler();
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Niklas Emil Lysdal
     */
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);

    }
    /**
     * @author Niklas Emil Lysdal
     * @author Bjarke Søderhamn Petersen
     */
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


    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Niklas Emil Lysdal
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientHandler, "/client").addInterceptors(clientInterceptor).setAllowedOrigins("*");

        registry.addHandler(hostHandler, "/host").setAllowedOrigins("*");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */
    private void initClientHandler() {
        clientHandler = new WebSocketHandler() { // Client

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
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

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                try {
                    String jSonText = message.getPayload().toString();
                    System.out.println(jSonText);
                    JsonNode json = JsonUtil.parser(jSonText);
                    String lobbyID = json.get("lobbyID").asText();
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

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.err.println("=== TRANSPORT ERROR ===");
                System.err.println("Session ID: " + session.getId());
                System.err.println("User: " + getUserFromSession(session));
                System.err.println("Error: " + exception.getMessage());
                //exception.printStackTrace();
                System.err.println("=====================");
            }

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
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

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }

        };
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Niklas Emil Lysdal
     * @author Asger Allin Jensen
     */
    private void initHostHandler() {
        hostHandler = new WebSocketHandler() { // host

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
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

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             * @author Asger Allin Jensen
             */
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

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             */
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

            }

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             */
            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // TODO: handle host disconnecting (probably message all clients that server is down)
            }

            /**
             * @author Karl Johannes Agerbo
             * @author Benjamin Benyo Endahl Hansen
             * @author Niklas Emil Lysdal
             */
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
