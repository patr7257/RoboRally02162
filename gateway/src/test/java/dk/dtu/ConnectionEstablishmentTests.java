package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Niklas Emil Lysdal
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class ConnectionEstablishmentTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @LocalServerPort
    int port;

    private StandardWebSocketClient clientSocket;
    private StandardWebSocketClient hostSocket;

    @BeforeEach
    void setup() {
        clientSocket = new StandardWebSocketClient();
        hostSocket = new StandardWebSocketClient();
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    void connectHostSuccessful() throws Exception {
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(1);

        var handler = new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.offer(session);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            }
        };

        URI uri = URI.create("ws://localhost:" + port + "/host");
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        CompletableFuture<WebSocketSession> future = hostSocket.execute(handler, headers, uri);
        WebSocketSession clientSession = future.get(5, TimeUnit.SECONDS);

        try {
            WebSocketSession established = sessions.poll(5, TimeUnit.SECONDS);
            assertThat(established).isNotNull();
            assertThat(clientSession.isOpen()).isTrue();
            assertThat(established.isOpen()).isTrue();
        } finally {
            if (clientSession != null && clientSession.isOpen()) {
                clientSession.close();
            }
        }
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    void connectClientSuccessful() throws Exception {
        // create user
        mockMvc.perform(post("/api/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\", \"passwordHash\":\"password\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("successful"));

        Map<String, String> map = new HashMap<>();
        map.put("username", "newuser");
        map.put("passwordHash", "password");
        String payload = mapper.writeValueAsString(map);
        // login user
        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("successful")).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = mapper.readTree(responseBody);
        String token = json.get("token").asText();
        // Establish and test connection
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(1);

        var handler = new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.offer(session);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            }
        };

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        URI uri = URI.create("ws://localhost:" + port + "/client?token=" + token);
        CompletableFuture<WebSocketSession> future = hostSocket.execute(handler, null, uri);
        WebSocketSession clientSession = future.get(5, TimeUnit.SECONDS);

        try {
            WebSocketSession established = sessions.poll(5, TimeUnit.SECONDS);
            assertThat(established).isNotNull();
            assertThat(clientSession.isOpen()).isTrue();
            assertThat(established.isOpen()).isTrue();
        } finally {
            if (clientSession != null && clientSession.isOpen()) {
                clientSession.close();
            }
        }
    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    void connectClientUnsuccessfulNoAuth() throws Exception {
        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(1);
        CompletableFuture<CloseStatus> closeStatusFuture = new CompletableFuture<>();
        var handler = new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.offer(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                closeStatusFuture.complete(closeStatus);
            }

        };

        URI uri = URI.create("ws://localhost:" + port + "/client");
        CompletableFuture<WebSocketSession> connectFuture = hostSocket.execute(handler, null, uri);
        WebSocketSession session = connectFuture.get(5, TimeUnit.SECONDS);
        assertThat(session).isNotNull(); //connect
        CloseStatus status = closeStatusFuture.get(5, TimeUnit.SECONDS); //kick
        assertThat(status.getCode()).isEqualTo(4001);

    }

    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    void connectClientUnsuccessfulInvalidAuth() throws Exception {

        ArrayBlockingQueue<WebSocketSession> sessions = new ArrayBlockingQueue<>(1);
        CompletableFuture<CloseStatus> closeStatusFuture = new CompletableFuture<>();
        var handler = new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.offer(session);
            }
            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                closeStatusFuture.complete(closeStatus);
            }

        };

        URI uri = URI.create("ws://localhost:" + port + "/client?token=someUserWithoutAccess");
        CompletableFuture<WebSocketSession> connectFuture = hostSocket.execute(handler, null, uri);
        WebSocketSession session = connectFuture.get(5, TimeUnit.SECONDS);
        assertThat(session).isNotNull(); //connect
        CloseStatus status = closeStatusFuture.get(5, TimeUnit.SECONDS); //kick
        assertThat(status.getCode()).isEqualTo(4001);

    }

}
