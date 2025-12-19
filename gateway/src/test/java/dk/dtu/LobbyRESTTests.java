package dk.dtu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


import dk.dtu.Util.TokenWebsocketContainer;


import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.model.Lobby;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;
import dk.dtu.util.TokenUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 * @author Niklas Emil Lysdal
 * @author Asger Allin Jensen
 */
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
    private DynamicUserDatabase userDatabase;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private Server server;

    @LocalServerPort
    int port;

    @BeforeEach
    void clean() {
        serverManager.resetLobbyCounterForTest();
        userDatabase.wipeUserDatabase();
    }

    @AfterEach
    void cleanafter() {
        userDatabase.wipeUserDatabase();
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testCreateLobby() throws Exception {
        String username = "TestUser";

        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username, mapper, mockMvc, port);
        String token = tokenWebsocket.getUserToken();
        WebSocketSession wsSession = tokenWebsocket.getSession();


        Map<String, String> createBodyMap1 = new HashMap<>();
        createBodyMap1.put("capacity", "6");
        createBodyMap1.put("lobbyName", "testName");
        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(createBodyMap1)))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // UUID lobbyUUID = UUID.fromString(lobbyID);

        String userID = tokenUtil.extractUserToken(token).userID();

        assertThat(server.getLobbiesForTest()).containsKey(lobbyID);
        assertThat(server.getLobbiesForTest().get(lobbyID).getPlayers().values())
                .contains(server.getClientsForTest().get(userID));

        wsSession.close();
    }
    // TODO: add test case for failure

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testJoinLobbySuccess() throws Exception {
        String hostUser = "HostUser";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(hostUser, mapper, mockMvc, port);
        String hostToken = tokenWebsocket.getUserToken();
        WebSocketSession hostSession = tokenWebsocket.getSession();

        String lobbyID = createLobby(hostToken, "testName", "6", mapper, mockMvc);


        String clientUser = "ClientUser";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(clientUser, mapper, mockMvc, port);
        String clientToken = tokenWebsocket2.getUserToken();
        WebSocketSession clientSession = tokenWebsocket2.getSession();


        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + clientToken)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isCreated())
                .andExpect(content().string(lobbyID));

        String clientID = tokenUtil.extractUserToken(clientToken).userID();
        String hostID  = tokenUtil.extractUserToken(hostToken).userID();
        assertThat(server.getLobbiesForTest().get(lobbyID).getPlayers().values())
                .contains(server.getClientsForTest().get(hostID));
        assertThat(server.getLobbiesForTest().get(lobbyID).getPlayers().values())
                .contains(server.getClientsForTest().get(clientID));

        hostSession.close();
        clientSession.close();

    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testJoinLobbyLockedLobby() throws Exception {
        String hostUser = "HostUser";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(hostUser, mapper, mockMvc, port);
        String hostToken = tokenWebsocket.getUserToken();
        WebSocketSession hostSession = tokenWebsocket.getSession();
        String lobbyID = createLobby(hostToken, "testName", "6", mapper, mockMvc);


        String clientUser = "ClientUser";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(clientUser, mapper, mockMvc, port);
        String clientToken = tokenWebsocket2.getUserToken();
        WebSocketSession clientSession = tokenWebsocket2.getSession();

        markReady(hostToken, lobbyID);


        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + hostToken)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + clientToken)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("LOBBY_LOCKED"));

    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testJoinLobbyUserNotConnected() throws Exception {
        String hostUser = "HostUser";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(hostUser, mapper, mockMvc, port);
        String hostToken = tokenWebsocket.getUserToken();
        WebSocketSession hostSession = tokenWebsocket.getSession();

        String lobbyID = createLobby(hostToken, "testName", "6", mapper, mockMvc);


        String clientUser = "ClientUser";
        String clientToken = createAndLoginUser(clientUser, mapper, mockMvc);
        // Intentional missing login

        Map<String, Object> joinBody = new HashMap<>();

        joinBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + clientToken)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("USER_NOT_CONNECTED"));

    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testJoinLobbyLobbyNotFound() throws Exception {
        String hostUser = "HostUser";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(hostUser, mapper, mockMvc, port);
        String hostToken = tokenWebsocket.getUserToken();
        WebSocketSession hostSession = tokenWebsocket.getSession();

        String lobbyID = createLobby(hostToken, "testName", "6", mapper, mockMvc);


        // UUID lobbyUUID = UUID.fromString(lobbyID);

        String clientUser = "ClientUser";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(clientUser, mapper, mockMvc, port);
        String clientToken = tokenWebsocket2.getUserToken();
        WebSocketSession clientSession = tokenWebsocket2.getSession();

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("lobbyID", "-1");

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + clientToken)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("LOBBY_NOT_FOUND"));

        hostSession.close();
        clientSession.close();

    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testStartLobby() throws Exception {
        String hostUser = "HostUser";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(hostUser, mapper, mockMvc, port);
        String hostToken = tokenWebsocket.getUserToken();
        WebSocketSession hostSession = tokenWebsocket.getSession();

        String lobbyID = createLobby(hostToken, "testName", "6", mapper, mockMvc);


        // UUID lobbyUUID = UUID.fromString(lobbyID);

        markReady(hostToken, lobbyID);


        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + hostToken)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());

        assertThat(server.getGameToLobbyForTest().values())
                .contains(lobbyID);

        String expectedGameId = "123e4567-e89b-12d3-a456-426614174000";

        assertThat(server.getGameToLobbyForTest())
                .containsEntry(expectedGameId, lobbyID);

        hostSession.close();
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Test
    public void seeLobbiesTest() throws Exception {
        String username = "TestUser";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username, mapper, mockMvc, port);
        String token = tokenWebsocket.getUserToken();
        WebSocketSession wsSession = tokenWebsocket.getSession();

        String lobbyID = createLobby(token, "testName", "6", mapper, mockMvc);
        String lobbyID2 = createLobby(token, "testName2", "6", mapper, mockMvc);
        String lobbyID3 = createLobby(token, "testName3", "6", mapper, mockMvc);

        markReady(token, lobbyID2);
        startLobby(token,lobbyID2);

        JsonNode jsonResult = JsonUtil.parser(mockMvc.perform(get("/api/lobby/seeLobbies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        JsonNode element0 = jsonResult.get(0);
        assertThat(element0.get("lobbyID").asText()).isEqualTo(lobbyID);
        assertThat(element0.get("lobbyName").asText()).isEqualTo("testName");
        assertThat(element0.get("capacity").asInt()).isEqualTo(6);
        assertThat(element0.get("playerCount").asInt()).isEqualTo(1);
        assertThat(!element0.get("isRunning").asBoolean());
        assertThat(element0.get("canJoin").asBoolean());
        //order is flipped because user 1 can't join lobby 2
        JsonNode element1 = jsonResult.get(1);
        assertThat(element1.get("lobbyID").asText()).isEqualTo(lobbyID3);
        assertThat(element1.get("lobbyName").asText()).isEqualTo("testName3");
        assertThat(element1.get("capacity").asInt()).isEqualTo(6);
        assertThat(element1.get("playerCount").asInt()).isEqualTo(1);
        assertThat(!element1.get("isRunning").asBoolean());
        assertThat(element1.get("canJoin").asBoolean());

        JsonNode element2 = jsonResult.get(2);
        assertThat(element2.get("lobbyID").asText()).isEqualTo(lobbyID2);
        assertThat(element2.get("lobbyName").asText()).isEqualTo("testName2");
        assertThat(element2.get("capacity").asInt()).isEqualTo(6);
        assertThat(element2.get("playerCount").asInt()).isEqualTo(1);
        assertThat(element2.get("isRunning").asBoolean());
        assertThat(!element2.get("canJoin").asBoolean());

        assertThat(jsonResult.path(3).isNull());
        wsSession.close();
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void leaveLobbySuccessfulTest() throws Exception {
        // create and login user
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        // create and login user 2
        String username2 = "TestUser2";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(username2, mapper, mockMvc, port);
        String token2 = tokenWebsocket2.getUserToken();
        WebSocketSession wsSession2 = tokenWebsocket2.getSession();
        // create lobby

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);
        joinLobby(token2, lobbyID);

        // user 2 join lobby


        // user 2 leave lobby
        Map<String, Object> leaveBody = new HashMap<>();
        leaveBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token2)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isNoContent());
        // check not in registry (should be good enough. no need to check websocket
        // directly.)
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        Assertions.assertFalse(lob.getPlayers().containsKey(username2));
        wsSession1.close();
        wsSession2.close();
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void leaveLobbyPlayerNotInLobbyTest() throws Exception {
        // create and login user 1
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);


        // create and login user 2
        String username2 = "TestUser2";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(username2, mapper, mockMvc, port);
        String token2 = tokenWebsocket2.getUserToken();
        WebSocketSession wsSession2 = tokenWebsocket2.getSession();

        Map<String, Object> leaveBody = new HashMap<>();
        leaveBody.put("lobbyID", lobbyID);
        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token2)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isConflict())
                .andExpect(content().string(Matchers.is("USER_NOT_IN_LOBBY")));

        wsSession1.close();
        wsSession2.close();
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void leaveLobbyEmptyLobbyTest() throws Exception {
        // test that lobby is deleted after becoming empty
        // create and login user 1
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);


        Map<String, Object> leaveBody = new HashMap<>();
        leaveBody.put("lobbyID", lobbyID);
        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isNoContent());
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        Assertions.assertNull(lob);
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void leaveLobbyPlayerIDTests() throws Exception {
        // create and login user
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        // create and login user 2
        String username2 = "TestUser2";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(username2, mapper, mockMvc, port);
        String token2 = tokenWebsocket2.getUserToken();
        WebSocketSession wsSession2 = tokenWebsocket2.getSession();

        String username3 = "TestUser3";
        TokenWebsocketContainer tokenWebsocket3 = createAndConnectUser(username3, mapper, mockMvc, port);
        String token3 = tokenWebsocket3.getUserToken();
        WebSocketSession wsSession3 = tokenWebsocket3.getSession();

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);
        // create lobby

        joinLobby(token2, lobbyID);
        joinLobby(token3, lobbyID);

        markReady(token1, lobbyID);
        markReady(token3, lobbyID);


        // user2 leave lobby
        Map<String, Object> leaveBody = new HashMap<>();
        leaveBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token2)
                        .content(mapper.writeValueAsString(leaveBody)))
                .andExpect(status().isNoContent());
        // start game
        startLobby(token1,lobbyID);


        // get playerID ID's
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        List<String> ids = lob.getPlayerIDs();
        // check that IDs are 1 and 2
        assertThat(ids).containsOnly("1", "2");

        wsSession1.close();
        wsSession2.close();
        wsSession3.close();
    }


    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    public void createClientIsNullTest() throws Exception {
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        String username2 = "TestUser2";
        String token2 = createAndLoginUser(username2, mapper, mockMvc);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token2)
                        .content(mapper.writeValueAsString(Map.of("userID", token2))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(lobbyID).isEqualTo("MISSING_WEBSOCKET_CONNECTION");
        wsSession1.close();
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    public void joinLobbyIDIsEmptyTest() throws Exception {
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        createLobby(token1, "testName", "6", mapper, mockMvc);

        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("lobbyID", "");
        String msg = mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();

        assertThat(msg).isEqualTo("LOBBY_ID_IS_EMPTY");
        wsSession1.close();
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    public void joinLobbyMissingToken() throws Exception {
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);


        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("lobbyID", lobbyID);
        String msg = mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + "")
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();



    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    public void joinLobbyIsFull() throws Exception {

        int maxPlayers = 6;
        String usernamePrefix = "TestUser";
        List<WebSocketSession> sessions = new ArrayList<>();

        // create and login user

        TokenWebsocketContainer tokenWebsocket1 = createAndConnectUser(usernamePrefix + "1", mapper, mockMvc, port);
        String token1 = tokenWebsocket1.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket1.getSession();
        sessions.add((wsSession1));

        String lobbyID = createLobby(token1, "testName", "" + maxPlayers, mapper, mockMvc);


        for (int i = 2; i <= maxPlayers; i++) {
            String username = usernamePrefix + i;


            TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username, mapper, mockMvc, port);
            String token = tokenWebsocket.getUserToken();
            WebSocketSession wsSession = tokenWebsocket.getSession();

            sessions.add(wsSession);

            joinLobby(token, lobbyID);

        }

        String token7 = createAndLoginUser(usernamePrefix + "7", mapper, mockMvc);
        WebSocketSession wsSession7 = connectWebSocket(token7, port,"LOGIN");

        Map<String, Object> joinBody7 = new HashMap<>();
        joinBody7.put("lobbyID", lobbyID);

        String msg7 = mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token7)
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

    /**
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */
    @Test
    public void mixtureReadinessTest() throws Exception {
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(2);
        int expectedUpdates = 5; // creation, join x2, mark ready x2
        int expectedLobbies = 2;
        final CountDownLatch latch = new CountDownLatch(expectedLobbies + expectedUpdates); //(new lobby, notify, notify)
        //expected messages: creation x2 (1 for each player), add player x1 for creation, add player x1 for player 2, update x2 for ready
        final AtomicInteger lobbiesMessages = new AtomicInteger(0);
        final AtomicInteger updateMessages = new AtomicInteger(0);

        var handler = new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.offer(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {

                try {
                    String jsonText = message.getPayload().toString();
                    JsonNode json = JsonUtil.parser(jsonText);
                    if (json.get("type").asText().equals("lobbies") && json.get("action").asText().equals("updatedLobbies")) {
                        lobbiesMessages.incrementAndGet();


                    } else if (json.get("type").asText().equals("lobby") && json.get("action").asText().equals("lobbyUpdate")) {
                        updateMessages.incrementAndGet();

                    }
                } catch (Exception e) {

                } finally {
                    latch.countDown();
                }


            }

        };

        // create and login user 1
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, handler, sessions, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();


        String username2 = "TestUser2";
        TokenWebsocketContainer tokenWebsocket2 = createAndConnectUser(username2, handler, sessions, mapper, mockMvc, port);
        String token2 = tokenWebsocket2.getUserToken();
        WebSocketSession wsSession2 = tokenWebsocket2.getSession();

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);


        joinLobby(token2, lobbyID);

        Map<String, Object> readyBody = new HashMap<>();
        readyBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/markReady")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(mapper.writeValueAsString(readyBody)))
                .andExpect(status().isNoContent());


        boolean success = latch.await(5, TimeUnit.SECONDS);
        if (!success) {
            Assertions.fail("LATCH timeout");
        }
        assertThat(lobbiesMessages.get()).isEqualTo(expectedLobbies);
        assertThat(updateMessages.get()).isEqualTo(expectedUpdates);

        JsonNode jsonResult1 = getLobbyInfo(token1, lobbyID);
        assertThat(jsonResult1.get("lobbyID").asText()).isEqualTo(lobbyID);
        JsonNode readiness1 = jsonResult1.get("readinessMap");
        assertThat(!readiness1.get("TestUser1").asBoolean());
        assertThat(readiness1.get("TestUser2").asBoolean());

        wsSession1.close();
        wsSession2.close();
    }

    /**
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */
    @Test
    public void onePlayerReadinessTest() throws Exception {
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(2);
        int expectedUpdates = 2; //initial add, and each ready/unready
        int expectedLobbies = 1;
        final CountDownLatch latch = new CountDownLatch(expectedUpdates + expectedLobbies); //(new lobby (including initial notify), notify, notify)
        final AtomicInteger updateMessages = new AtomicInteger(0);
        final AtomicInteger lobbiesMessages = new AtomicInteger(0);
        var handler = new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.offer(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {

                try {
                    String jsonText = message.getPayload().toString();
                    JsonNode json = JsonUtil.parser(jsonText);
                    if (json.get("type").asText().equals("lobbies") && json.get("action").asText().equals("updatedLobbies")) {
                        lobbiesMessages.incrementAndGet();


                    } else if (json.get("type").asText().equals("lobby") && json.get("action").asText().equals("lobbyUpdate")) {
                        updateMessages.incrementAndGet();

                    }
                } catch (Exception e) {

                } finally {
                    latch.countDown();
                }


            }

        };

        // create and login user 1

        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, handler, sessions, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);


        // mark player 1 as ready
        Map<String, Object> readyBody = new HashMap<>();
        readyBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/markReady")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(mapper.writeValueAsString(readyBody)))
                .andExpect(status().isNoContent());

        boolean success = latch.await(5, TimeUnit.SECONDS);
        if (!success) {
            Assertions.fail("LATCH timeout");
        }
        assertThat(lobbiesMessages.get()).isEqualTo(expectedLobbies);
        assertThat(updateMessages.get()).isEqualTo(expectedUpdates);
        JsonNode jsonResult1 = getLobbyInfo(token1, lobbyID);
        assertThat(jsonResult1.get("lobbyID").asText()).isEqualTo(lobbyID);
        JsonNode readiness1 = jsonResult1.get("readinessMap");
        assertThat(readiness1.get("TestUser1").asBoolean());
        wsSession1.close();
    }

    /**
     * @author Asger Allin Jensen
     * @author Niklas Emil Lysdal
     */
    @Test
    public void unreadyPlayerTest() throws Exception {
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(2);
        int expectedUpdates = 3; //initial add, and each ready/unready
        int expectedLobbies = 1;
        final AtomicInteger totalMessages = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(expectedUpdates + expectedLobbies); //(new lobby (including initial notify), notify, notify)
        final AtomicInteger updateMessages = new AtomicInteger(0);
        final AtomicInteger lobbiesMessages = new AtomicInteger(0);
        var handler = new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.offer(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                try {
                    totalMessages.incrementAndGet();
                    String jsonText = message.getPayload().toString();
                    JsonNode json = JsonUtil.parser(jsonText);
                    if (json.get("type").asText().equals("lobbies") && json.get("action").asText().equals("updatedLobbies")) {
                        lobbiesMessages.incrementAndGet();

                    } else if (json.get("type").asText().equals("lobby") && json.get("action").asText().equals("lobbyUpdate")) {
                        updateMessages.incrementAndGet();
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            }

        };

        // create and login user 1
        String username1 = "TestUser1";
        TokenWebsocketContainer tokenWebsocket = createAndConnectUser(username1, handler, sessions, mapper, mockMvc, port);
        String token1 = tokenWebsocket.getUserToken();
        WebSocketSession wsSession1 = tokenWebsocket.getSession();

        // create lobby
        String lobbyID = createLobby(token1, "testName", "6", mapper, mockMvc);


        // mark player 1 as ready
        Map<String, Object> readyBody = new HashMap<>();
        readyBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/markReady")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(mapper.writeValueAsString(readyBody)))
                .andExpect(status().isNoContent());

        JsonNode jsonResult1 = getLobbyInfo(token1, lobbyID);
        assertThat(jsonResult1.get("lobbyID").asText()).isEqualTo(lobbyID);
        JsonNode readiness1 = jsonResult1.get("readinessMap");
        assertThat(readiness1.get("TestUser1").asBoolean());

        // mark player 1 as unready
        Map<String, Object> notReadyBody = new HashMap<>();
        readyBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/markNotReady")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token1)
                        .content(mapper.writeValueAsString(readyBody)))
                .andExpect(status().isNoContent());
        boolean success = latch.await(5, TimeUnit.SECONDS);
        if (!success) {
            Assertions.fail("LATCH timeout");
        }
        assertThat(totalMessages.get()).isEqualTo(expectedUpdates + expectedLobbies);
        assertThat(lobbiesMessages.get()).isEqualTo(expectedLobbies);
        assertThat(updateMessages.get()).isEqualTo(expectedUpdates);

        JsonNode jsonResult2 = getLobbyInfo(token1, lobbyID);
        assertThat(jsonResult2.get("lobbyID").asText()).isEqualTo(lobbyID);
        JsonNode readiness2 = jsonResult2.get("readinessMap");
        assertThat(!readiness2.get("TestUser1").asBoolean());

        wsSession1.close();

    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     * @author Bjarke Søderhamn Petersen
     * @author Niklas Emil Lysdal
     */
    public static String createAndLoginUser(String username, ObjectMapper mapper, MockMvc mockMvc) throws Exception {
        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                Map.of("username", username, "passwordHash", "password"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                Map.of("username", username, "passwordHash", "password"))))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     */
    public static String createLobby(String token, String lobbyName, String capacity, ObjectMapper mapper, MockMvc mockMvc) throws Exception {
        Map<String, String> createBodyMap1 = new HashMap<>();
        createBodyMap1.put("capacity", capacity);
        createBodyMap1.put("lobbyName", lobbyName);
        // create lobby
        return mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(createBodyMap1)))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     */
    private String joinLobby(String token, String lobbyID) throws Exception {
        Map<String, Object> joinBody = new HashMap<>();
        joinBody.put("lobbyID", lobbyID);

        return mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(joinBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse()
                .getContentAsString();
    }

    /**
     * @author Niklas Emil Lysdal
     */
    private void markReady(String token, String lobbyID) throws Exception {
        Map<String, Object> readyBody = new HashMap<>();
        readyBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/markReady")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(readyBody)))
                .andExpect(status().isNoContent());

    }

    /**
     * @author Niklas Emil Lysdal
     */
    private void markNotReady(String token, String lobbyID) throws Exception {
        Map<String, Object> notReadyBody = new HashMap<>();
        notReadyBody.put("lobbyID", lobbyID);

        mockMvc.perform(post("/api/lobby/markNotReady")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(notReadyBody)))
                .andExpect(status().isNoContent());

    }

    /**
     * @author Niklas Emil Lysdal
     */
    private TokenWebsocketContainer createAndConnectUser(String userName, ObjectMapper mapper, MockMvc mockMvc, int port) throws Exception {
        String token = createAndLoginUser(userName, mapper, mockMvc);
        //sleep for safety for concurrency.
        Thread.sleep(50);
        WebSocketSession session = connectWebSocket(token, port,"LOGIN");
        Thread.sleep(50);
        return new TokenWebsocketContainer(session, token);

    }

    /**
     * @author Niklas Emil Lysdal
     */
    private TokenWebsocketContainer createAndConnectUser(String userName, AbstractWebSocketHandler handler, ArrayBlockingQueue<WebSocketSession> sessions, ObjectMapper mapper, MockMvc mockMvc, int port) throws Exception {
        String token = createAndLoginUser(userName, mapper, mockMvc);
        //sleep for safety for concurrency.
        Thread.sleep(50);
        WebSocketSession session = connectWebSocket(token, handler, sessions, port,"LOGIN");
        Thread.sleep(50);
        return new TokenWebsocketContainer(session, token);

    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     */
    public static WebSocketSession connectWebSocket(String token, int port,String reason) throws Exception {
        // Establish WebSocket connection for client
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(1);
        var handler = new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.offer(session);
            }
        };

        URI uri = URI.create("ws://localhost:" + port + "/client?token=" + token+"&reason="+reason);
        new StandardWebSocketClient().execute(handler, uri.toString());
        WebSocketSession wsSession = sessions.poll(10, TimeUnit.SECONDS);

        assertThat(wsSession).isNotNull();
        return wsSession;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    private WebSocketSession connectWebSocket(String token, AbstractWebSocketHandler handler,
                                              ArrayBlockingQueue<WebSocketSession> sessions, int port, String reason) throws Exception {
        URI uri = URI.create("ws://localhost:" + port + "/client?token=" + token+"&reason="+reason);
        new StandardWebSocketClient().execute(handler, uri.toString());

        WebSocketSession wsSession = sessions.poll(10, TimeUnit.SECONDS);
        assertThat(wsSession).isNotNull();
        return wsSession;
    }

    /**
     * @author Niklas Emil Lysdal
     */
    private JsonNode getLobbyInfo(String token, String lobbyID) throws Exception {
        Map<String, String> infoBody = new HashMap<>();
        infoBody.put("lobbyID", lobbyID);
        JsonNode jsonResult = JsonUtil.parser(mockMvc.perform(post("/api/lobby/lobbyInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(infoBody)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return jsonResult;
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Karl Johannes Agerbo
     */
    private void startLobby(String token, String lobbyID) throws Exception {
        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());
    }
}
