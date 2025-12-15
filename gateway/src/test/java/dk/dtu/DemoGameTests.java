package dk.dtu;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;
import dk.dtu.util.TokenUtil;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.web.socket.WebSocketSession;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import static dk.dtu.LobbyRESTTests.connectWebSocket;
import static dk.dtu.LobbyRESTTests.createAndLoginUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(HostConfig.class)
@AutoConfigureMockMvc
public class DemoGameTests {

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
        userDatabase.wipeUserDatabase();
    }

    @AfterEach
    void cleanafter() {
        userDatabase.wipeUserDatabase();
    }

    /**
     * @author Karl Johannes Agerbo
     */
    @Test
    public void testCreateAndStartDemo() throws Exception {
        String username = "TestUser";
        String token = createAndLoginUser(username, mapper, mockMvc);

        Thread.sleep(50);
        WebSocketSession wsSession = connectWebSocket(token, port, "LOGIN");
        Thread.sleep(50);

        String templates = mockMvc.perform(get("/api/demo/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> templateNames = JsonUtil.toList(templates);
        String firstTemplate = templateNames.getFirst();

        String lobID = mockMvc.perform(post("/api/lobby/createAndStartDemo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(mapper.writeValueAsString(Map.of("demoTemplate", firstTemplate))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(lobID).contains("D");
        assertThat(serverManager.getLobbiesForTest().keySet()).doesNotContain(lobID);
        assertThat(serverManager.getLoadedLobbiesForTest().keySet()).doesNotContain(lobID);
        assertThat(serverManager.getDemoLobbiesForTest().keySet()).contains(lobID);
    }

}
