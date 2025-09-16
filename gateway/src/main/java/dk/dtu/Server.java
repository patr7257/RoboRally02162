package dk.dtu;

/*
Author(s): Niklas, Karl, Benjamin
 */

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;

import java.util.*;

@SpringBootApplication
@EnableWebSocket
@RestController
@RequestMapping("/api")
public class Server implements WebSocketConfigurer {

    private final LobbyFactory lobFactory = new LobbyFactory();
    private final Map<String, Client> clients = new HashMap<>();//Currently username->client, in future might be unique identifier.
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler() { //Client
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                User user = (User) session.getAttributes().get("user");
                Client client = new Client(session.getId(),user,session);
                clients.put(client.getUsername(),client); //TODO: change to ID
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String jSonText = message.getPayload().toString();

                JsonNode json = JsonUtil.parser(jSonText);
                UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
                String userID = (String) session.getAttributes().get("userID");
                Lobby lob = lobbies.get(lobbyID); //TODO: check for valid ID
                lob.handleClientMessage(userID, json.get("payload")); //TODO: Check that toString() is correct
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                exception.printStackTrace();
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                //leaveLobby(session);
                //TODO: leave lobbies and leave clients map
                System.out.println("Client disconnected: " + session.getId());
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }

        }, "/client").addInterceptors(clientInterceptor).setAllowedOrigins("*");

        registry.addHandler(new WebSocketHandler() { //host
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                host.setSession(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String jSonText = message.getPayload().toString();
                JsonNode json = JsonUtil.parser(jSonText);
                String gameID = json.get("gameID").asText();

                String lobbyID = gameToLobby.get(gameID); //TODO: check for valid ID
                Lobby lob = lobbies.get(lobbyID);
                lob.handleHostMessage(json);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                //TODO: handle host disconnecting (probably message all clients that server is down)
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

    /*TODO: lobby endpoints should be moved to own file
                For this the used maps should be turned into beans in a class that is passed to the constructor of both the server and the created class.
                 */
    @PostMapping("/lobby/create") //returns lobbyID.
    public ResponseEntity<String> createLobby(@RequestBody JsonNode json) { //TODO: add authorization
        String username = json.get("username").asText();
        Client creator = clients.get(username); //TODO: make check that person is connected to websocket (essentially check if in clients)
        Lobby lob = lobFactory.createLobby(creator,this.host);
        lobbies.put(lob.getLobbyID(),lob);
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
        //TODO: add error checking
    }

    @PostMapping("/lobby/join")
    public ResponseEntity<String> joinLobby(@RequestBody JsonNode json) {
        String username = json.get("username").asText();
        //TODO: add error handling
        String lobbyID = json.get("lobbyID").asText();
        //UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        Client client = clients.get(username);
        Lobby lob = lobbies.get(lobbyID);
        lob.addPlayer(client);
        //TODO: add check if lobby is full and return success/failure message
        //success message
        return ResponseEntity.status(HttpStatus.CREATED).body(lob.getLobbyID().toString());
   }

    @PostMapping("/lobby/start") //TODO: add check that websocket connection is running
    public void startLobby(@RequestBody JsonNode json) {
        //UUID lobbyID = UUID.fromString(json.get("lobbyID").asText());
        String lobbyID = json.get("lobbyID").asText();
        //TODO: add valid ID checking
        Lobby lob = lobbies.get(lobbyID);
        lob.startGame(gameId -> gameToLobby.put(gameId, lob.getLobbyID()));

    }

    @GetMapping("/lobby/seeLobbies")
    public ResponseEntity<String> seeLobbies() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Lobby lobby : lobbies.values()) { //TODO: Do this in Lobby
            Map<String, Object> lobbyInfo = new HashMap<>();
            lobbyInfo.put("lobbyID", lobby.getLobbyID());
            result.add(lobbyInfo);
        }

        String json = JsonUtil.toJson(result);

        return ResponseEntity.ok(json);
    }
}
