package dk.dtu;

/*
Author(s): Karl
 */

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dtu.interfaces.UserDatabase;
import org.hamcrest.Matchers;
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
import java.util.HashMap;
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
    private Server server;

    @LocalServerPort
    int port;


    @Test
    public void testCreateLobby() throws Exception {
        String username = "TestUser";

        String token = createAndLoginUser(username);

        WebSocketSession wsSession = connectWebSocket(token);

        String lobbyID = mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        //UUID lobbyUUID = UUID.fromString(lobbyID);

        assertThat(server.getLobbies()).containsKey(lobbyID);
        assertThat(server.getLobbies().get(lobbyID).getPlayers().values())
                .contains(server.getClients().get(username));

        wsSession.close();
    }
    //TODO: add test case for failure


    @Test
    public void testJoinLobbySuccess() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);
        WebSocketSession hostSession = connectWebSocket(hostToken);

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

        assertThat(server.getLobbies().get(lobbyID).getPlayers().values())
                .contains(server.getClients().get(hostUser));
        assertThat(server.getLobbies().get(lobbyID).getPlayers().values())
                .contains(server.getClients().get(clientUser));

        hostSession.close();
        clientSession.close();

    }

    @Test
    public void testJoinLobbyLockedLobby() throws Exception {
        String hostUser = "HostUser";
        String hostToken = createAndLoginUser(hostUser);
        WebSocketSession hostSession = connectWebSocket(hostToken);

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
        WebSocketSession hostSession = connectWebSocket(hostToken);

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
        WebSocketSession hostSession = connectWebSocket(hostToken);

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
        joinBody.put("lobbyID", "");

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
        WebSocketSession hostSession = connectWebSocket(hostToken);

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

        assertThat(server.getGameToLobby().values())
                .contains(lobbyID);

        String expectedGameId = "123e4567-e89b-12d3-a456-426614174000";

        assertThat(server.getGameToLobby())
                .containsEntry(expectedGameId, lobbyID);

        hostSession.close();
    }

    @Test
    public void seeLobbiesTest() throws Exception {
        String username = "TestUser";
        String token = createAndLoginUser(username);
        WebSocketSession wsSession = connectWebSocket(token);

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
        WebSocketSession wsSession = sessions.poll(2, TimeUnit.SECONDS);

        assertThat(wsSession).isNotNull();
        return wsSession;
    }

}
