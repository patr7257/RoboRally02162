package dk.dtu;


import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.dto.ClientConnectReason;
import dk.dtu.dto.ClientDisconnectReason;
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
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;

import java.util.*;

/**
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 * @author Niklas Emil Lysdal
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
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
        return (String) session.getAttributes().get("token");
    }

    private ClientConnectReason getReasonFromSession(WebSocketSession session) {
        return (ClientConnectReason) session.getAttributes().get("reason");

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
                if (session.getAttributes().containsKey("error")) {
                    String errorType = (String) session.getAttributes().get("error");

                    if (errorType.equals("not logged in")) {
                        session.close(new CloseStatus(4001,"NOT_LOGGED_IN"));
                        return;
                    }

                    if (errorType.equals("invalid token")) {
                        session.close(new CloseStatus(4001,"INVALID_TOKEN"));
                        return;
                    }
                    if (errorType.equals("unknown user")) {
                        session.close(new CloseStatus(4001,"UNKNOWN_USER"));
                        return;
                    }
                }
                String token = getTokenFromSession(session);
                User user = (User) session.getAttributes().get("user");
                if (serverManager.existsClient(user.getUserID())) {

                    Client c = serverManager.getClient(user.getUserID());
                    ClientConnectReason reason = getReasonFromSession(session);
                    c.handleConnect(session,reason);
                } else {

                    serverManager.createClient(user,session);
                }
                System.out.println("=== WebSocket CONNECTED ===");
                System.out.println("Session ID: " + session.getId());
                System.out.println("User: " + user.getName());
                System.out.println("Session state: " + session.isOpen());
                System.out.println("Connection reason"+getReasonFromSession(session));
                System.out.println("========================");


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
                    String lobbyID = json.get("lobbyID").asText();
                    User user = (User) session.getAttributes().get("user");
                    String userID = user.getUserID();
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
            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                if (closeStatus.getCode()==4001 ||closeStatus.getCode()==4002) {
                    return; //we closed it and kicked the user
                }
                User u = (User) session.getAttributes().get("user");
               if (u ==null) {
                   System.err.println("[WEBSOCKET] Missing user");
                   return;
               }
                Client c = serverManager.getClient(u.getUserID());
               if (c==null){
                   System.err.println("[WEBSOCKET] Session closed with client not registered.");
               }
                if (closeStatus.getCode() ==1000) {
                    c.handleDisconnect(ClientDisconnectReason.LOGOUT);
                } else {
                    c.handleDisconnect(ClientDisconnectReason.CONNECTION_LOSS);
                }


                System.err.println("=== WebSocket CLOSED ===");
                System.err.println("Session ID: " + session.getId());
                System.err.println("User: " + getUserFromSession(session));
                System.err.println("Close code: " + closeStatus.getCode());
                System.err.println("Close reason: " + closeStatus.getReason());
                System.err.println("Was clean: " + closeStatus);
                System.err.println("Close triggered from:");
                //Thread.dumpStack();
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
