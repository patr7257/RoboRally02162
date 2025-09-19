package dk.dtu;

/*
Author(s): Niklas, Karl, Benjamin
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@SpringBootApplication
@EnableWebSocket
@RestController
@RequestMapping("/api")
public class Server implements WebSocketConfigurer, CommandLineRunner { // TODO: after host connects remove

    private final LobbyFactory lobFactory = new LobbyFactory();
    private final Map<String, Client> clients = new HashMap<>();// Currently username->client, in future might be unique
                                                                // identifier.
    private final Host host;
    private final Map<String, Lobby> lobbies = new HashMap<>();
    private final Map<String, String> gameToLobby = new HashMap<>();
    private final ClientHandshakeInterceptor clientInterceptor;

    @Autowired
    public Server(ClientHandshakeInterceptor cliHandInt, Host host) {
        this.clientInterceptor = cliHandInt;
        this.host = host;
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

                    String lobbyID = gameToLobby.get(gameID); // TODO: check for valid ID
                    Lobby lob = lobbies.get(lobbyID);
                    lob.handleHostMessage(json);

                }
            }, hostUrl).get();

            host.setSession(session);
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
                clients.put(client.getUsername(), client); // TODO: change to ID
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
                    Lobby lob = lobbies.get(lobbyID); // TODO: check for valid ID
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
                host.setSession(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String jSonText = message.getPayload().toString();
                JsonNode json = JsonUtil.parser(jSonText);
                String gameID = json.get("meta").get("game").get("gameID").asText();

                String lobbyID = gameToLobby.get(gameID); // TODO: check for valid ID
                Lobby lob = lobbies.get(lobbyID);
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
        return lobbies;
    }

    public Map<String, String> getGameToLobby() {
        return gameToLobby;
    }

    public Map<String, Client> getClients() {
        return clients;
    }

    /*
     * TODO: lobby endpoints should be moved to own file
     * For this the used maps should be turned into beans in a class that is passed
     * to the constructor of both the server and the created class.
     */
    @PostMapping("/lobby/create") // returns lobbyID.
    public ResponseEntity<String> createLobby(@RequestBody JsonNode json) { // TODO: add authorization
        String username = json.get("username").asText();
        Client creator = clients.get(username); // TODO: make check that person is connected to websocket (essentially
                                                // check if in clients)
        Lobby lob = lobFactory.createLobby(creator, this.host);
        lobbies.put(lob.getLobbyID(), lob);
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
        // TODO: add error checking
    }

    @PostMapping("/lobby/join")
    public ResponseEntity<String> joinLobby(@RequestBody JsonNode json) {
        String username = json.get("username").asText();
        // TODO: add error handling
        String lobbyID = json.get("lobbyID").asText();
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        Client client = clients.get(username);
        Lobby lob = lobbies.get(lobbyID);
        lob.addPlayer(client);
        // TODO: add check if lobby is full and return success/failure message
        // success message
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
    }

    @PostMapping("/lobby/start") // TODO: add check that websocket connection is running
    public void startLobby(@RequestBody JsonNode json) {
        // UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        String lobbyID = json.get("lobbyID").asText();
        // TODO: add valid ID checking
        Lobby lob = lobbies.get(lobbyID);
        // TODO: start game through lobby
        UUID gameID = host.startGame(lob.getPlayers().size(), 10); // TODO: Change the boardsize to be decided by the
                                                                   // client
        gameToLobby.put(gameID.toString(), lob.getLobbyID());
        lob.startGame(gameID.toString());
    }

    @GetMapping("/lobby/seeLobbies")
    public ResponseEntity<String> seeLobbies() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Lobby lobby : lobbies.values()) { // TODO: Do this in Lobby
            Map<String, Object> lobbyInfo = new HashMap<>();
            lobbyInfo.put("lobbyID", lobby.getLobbyID());
            result.add(lobbyInfo);
        }

        String json = JsonUtil.toJson(result);

        return ResponseEntity.ok(json);
    }
}
