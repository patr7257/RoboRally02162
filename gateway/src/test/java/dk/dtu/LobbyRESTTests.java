package dk.dtu;

/*
Author(s): Karl, Benjamin
 */

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dtu.interfaces.UserDatabase;
import dk.dtu.model.Lobby;
import dk.dtu.shared.ServerManager;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(HostConfig.class)
@AutoConfigureMockMvc
public class LobbyRESTTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserDatabase userDatabase;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private Server server;

    @LocalServerPort
    int port;

    @Test
    public void testCreateLobby() throws Exception {
        String username = "TestUser";

        String token = createAndLoginUser(username);

        // We sleep here after creating the user.
        // This is probably a result of concurrency?
        // Making the thread sleep fixed the automatic tests on gitHub.
        Thread.sleep(50);

        WebSocketSession wsSession = connectWebSocket(token);

        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        assertThat(server.getLobbiesForTest()).containsKey(lobbyID);
        assertThat(server.getLobbiesForTest().get(lobbyID).getPlayers().values())
                .contains(server.getClientsForTest().get(username));

        wsSession.close();
    }
    //TODO: add test case for failure


    @Test
    public void testJoinLobbySuccess() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);

        Thread.sleep(50);

        WebSocketSession hostSession = connectWebSocket(hostToken);

        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", hostUser))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        String clientUser = "ClientUser";
        String clientToken = createAndLoginUser(clientUser);
        WebSocketSession clientSession = connectWebSocket(clientToken);

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", clientUser);
        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isCreated())
                .andExpect(content().string(lobbyID));

        assertThat(server.getLobbiesForTest().get(lobbyID).getPlayers().values())
                .contains(server.getClientsForTest().get(hostUser));
        assertThat(server.getLobbiesForTest().get(lobbyID).getPlayers().values())
                .contains(server.getClientsForTest().get(clientUser));

        hostSession.close();
        clientSession.close();

    }

    @Test
    public void testJoinLobbyLockedLobby() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);

        Thread.sleep(50);

        WebSocketSession hostSession = connectWebSocket(hostToken);

        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", hostUser))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        String clientUser = "ClientUser";
        String clientToken = createAndLoginUser(clientUser);
        WebSocketSession clientSession = connectWebSocket(clientToken);

        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());


        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", clientUser);
        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("LOBBY_LOCKED"));





    }

    @Test
    public void testJoinLobbyUserNotConnected() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);

        Thread.sleep(50);

        WebSocketSession hostSession = connectWebSocket(hostToken);

        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", hostUser))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        String clientUser = "ClientUser";
        String clientToken = createAndLoginUser(clientUser);
        //Intentional missing login


        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", clientUser);
        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("USER_NOT_CONNECTED"));





    }
    @Test
    public void testJoinLobbyLobbyNotFound() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);

        Thread.sleep(50);

        WebSocketSession hostSession = connectWebSocket(hostToken);

        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", hostUser))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        String clientUser = "ClientUser";
        String clientToken = createAndLoginUser(clientUser);
        WebSocketSession clientSession = connectWebSocket(clientToken);

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", clientUser);
        joinBody.put("lobbyID", "-1");

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("LOBBY_NOT_FOUND"));


        hostSession.close();
        clientSession.close();





    }
    @Test
    public void testStartLobby() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);

        Thread.sleep(50);

        WebSocketSession hostSession = connectWebSocket(hostToken);

        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", hostUser))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());

        assertThat(server.getGameToLobbyForTest().values())
                .contains(lobbyID);

        String expectedGameId = "123e4567-e89b-12d3-a456-426614174000";

        assertThat(server.getGameToLobbyForTest())
                .containsEntry(expectedGameId, lobbyID);

        hostSession.close();
    }

    @Test
    public void seeLobbiesTest() throws Exception {
        String username = "TestUser";
        String token = createAndLoginUser(username);

        Thread.sleep(50);

        WebSocketSession wsSession = connectWebSocket(token);

        Thread.sleep(50);

        String lobbyID1 = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String lobbyID2 = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID2))))
                .andExpect(status().isOk());

        String lobbyID3 = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();


        String expectedJson = "[{\"lobbyID\":\"" + lobbyID1 + "\"}, {\"lobbyID\":\"" + lobbyID3 + "\"}]";

        mockMvc.perform(get("/api/lobby/seeLobbies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        wsSession.close();
    }

    @Test
    public void leaveLobbySuccessfulTest() throws Exception {
        //create and login user
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);
        //create and login user 2
        String username2 = "TestUser2";

        String token2 = createAndLoginUser(username2);
        Thread.sleep(50);
        WebSocketSession wsSession2 = connectWebSocket(token2);
        Thread.sleep(50);
        //create lobby
        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username1))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        //user 2 join lobby
        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", username2);
        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isCreated())
                .andExpect(content().string(lobbyID));
        //user 2 leave lobby
        Map<String,Object> leaveBody = new HashMap<>();
        leaveBody.put("username", username2);
        leaveBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isNoContent());
        // check not in registry (should be good enough. no need to check websocket directly.)
        Lobby lob = serverManager.getLobbyFromID(lobbyID);
        Assertions.assertFalse(lob.getPlayers().containsKey(username2));

    }

    @Test
    public void leaveLobbyPlayerNotInLobbyTest() throws Exception {
        //create and login user 1
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        //create lobby
        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username1))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        //create and login user 2
        String username2 = "TestUser2";
        String token2 = createAndLoginUser(username2);
        Thread.sleep(50);
        WebSocketSession wsSession2 = connectWebSocket(token2);
        Thread.sleep(50);


        Map<String,Object> leaveBody = new HashMap<>();
        leaveBody.put("username", username2);
        leaveBody.put("lobbyID", lobbyID);
        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isConflict())
                .andExpect(content().string(Matchers.is("USER_NOT_IN_LOBBY")));

    }

    @Test
    public void leaveLobbyEmptyLobbyTest() throws Exception {
        System.out.println("LEAVELOBBYEMPTYLOBBYTEST");
        //test that lobby is deleted after becoming empty
        //create and login user 1
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        //create lobby
        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username1))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String,Object> leaveBody = new HashMap<>();
        leaveBody.put("username", username1);
        leaveBody.put("lobbyID", lobbyID);
        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isNoContent());
        Lobby lob = serverManager.getLobbyFromID(lobbyID);
        Assertions.assertNull(lob);
    }


    @Test
    public void leaveLobbyPlayerIDTests() throws Exception {
        //create and login user
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        //create and login user 2
        String username2 = "TestUser2";
        String token2 = createAndLoginUser(username2);
        Thread.sleep(50);
        WebSocketSession wsSession2 = connectWebSocket(token2);
        Thread.sleep(50);
        String username3 = "TestUser3";
        String token3 = createAndLoginUser(username3);
        Thread.sleep(50);
        WebSocketSession wsSession3 = connectWebSocket(token3);
        Thread.sleep(50);

        //create lobby


        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username1))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();


        //user2 join lobby
        Map<String, Object> joinBody2 = new HashMap<>();
        joinBody2.put("username", username2);
        joinBody2.put("lobbyID", lobbyID);
        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody2)))
                .andExpect(status().isCreated())
                .andExpect(content().string(lobbyID));
        //user3 join lobby
        Map<String, Object> joinBody3 = new HashMap<>();
        joinBody3.put("username", username3);
        joinBody3.put("lobbyID", lobbyID);
        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody3)))
                .andExpect(status().isCreated())
                .andExpect(content().string(lobbyID));


        //user2 leave lobby
        Map<String,Object> leaveBody = new HashMap<>();
        leaveBody.put("username", username2);
        leaveBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isNoContent());
        //start game
        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());

        //get playerID ID's
        Lobby lob = serverManager.getLobbyFromID(lobbyID);
        List<String> ids = lob.getPlayerIDs();
        //check that IDs are 1 and 2
        assertThat(ids).containsOnly("1","2");

    }

    @Test
    public void createUsernameIsEmptyTest() throws Exception {
        //create and login user
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username",""))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(lobbyID).isEqualTo("USERNAME_IS_EMPTY");
    }

    @Test
    public void createClientIsNullTest() throws Exception {
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username","testUser2"))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(lobbyID).isEqualTo("CLIENT_IS_NULL");
    }

    @Test
    public void joinLobbyIDIsEmptyTest() throws Exception{
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username",username1))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", username1);
        joinBody.put("lobbyID", "");
        String msg = mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();

        assertThat(msg).isEqualTo("LOBBY_ID_IS_EMPTY");

    }

    @Test
    public void joinLobbyUsernameIsEmptyTest() throws Exception{
        String username1 = "TestUser1";
        String token1 = createAndLoginUser(username1);
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username1))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("username", "");
        joinBody.put("lobbyID", lobbyID);
        String msg = mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();

        assertThat(msg).isEqualTo("USERNAME_IS_EMPTY");

    }

    @Test
    public void joinLobbyIsFull() throws Exception{

        int maxPlayers = 6;
        String usernamePrefix = "TestUser";
        List<WebSocketSession> sessions = new ArrayList<>();


        //create and login user

        String token1 = createAndLoginUser(usernamePrefix + "1");
        Thread.sleep(50);
        WebSocketSession wsSession1 = connectWebSocket(token1);
        Thread.sleep(50);
        sessions.add((wsSession1));

        //create lobby
        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", usernamePrefix + "1"))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (int i = 2; i <= maxPlayers; i++){
            String username = usernamePrefix + i;
            String token = createAndLoginUser(username);
            Thread.sleep(50);
            WebSocketSession wsSession = connectWebSocket(token);
            Thread.sleep(50);
            sessions.add(wsSession);

            Map<String, Object> joinBody = new HashMap<>();
            joinBody.put("username", username);
            joinBody.put("lobbyID", lobbyID);
            mockMvc.perform(post("/api/lobby/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(joinBody)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(lobbyID));
        }

        String token7 = createAndLoginUser(usernamePrefix + "7");
        WebSocketSession wsSession7 = connectWebSocket(token7);

        Map<String, Object> joinBody7 = new HashMap<>();
        joinBody7.put("username", usernamePrefix + "7");
        joinBody7.put("lobbyID", lobbyID);

        String msg7 = mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(joinBody7)))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(msg7).isEqualTo("LOBBY_IS_FULL");

        for (WebSocketSession session : sessions) {
            session.close();
        }
        wsSession7.close();


    }

    private String createAndLoginUser(String username) throws Exception {
        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "passwordHash", "password"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "passwordHash", "password"))))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readTree(loginResult.getResponse().getContentAsString()).get("username").asText(); //TODO: change to token when the clientHandshake actually checks tokens instead of usernames.
    }

    private WebSocketSession connectWebSocket(String token) throws Exception {
        // Establish WebSocket connection for client
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(1);
        var handler = new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.offer(session);
            }
        };

        URI uri = URI.create("ws://localhost:" + port + "/client?token=" + token);
        new StandardWebSocketClient().execute(handler, uri.toString());
        WebSocketSession wsSession = sessions.poll(10, TimeUnit.SECONDS);

        assertThat(wsSession).isNotNull();
        return wsSession;
    }

}
