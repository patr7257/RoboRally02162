
package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.User;
import dk.dtu.model.database.DynamicGameDatabase;
import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

import static dk.dtu.LobbyRESTTests.connectWebSocket;
import static dk.dtu.LobbyRESTTests.createAndLoginUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(HostConfig.class)
@AutoConfigureMockMvc

/**
 @author Bjarke Søderhamn Petersen
 @author Benjamin Benyo Endahl Hansen
 @author Karl Johannes Agerbo
 */


public class GameDatabaseTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private DynamicGameDatabase gameDatabase;

    @Autowired
    private DynamicUserDatabase userDatabase;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private Server server;

    @LocalServerPort
    int port;

    private final List<WebSocketSession> sessions = new ArrayList<>();
    private final List<String> userIDs = new ArrayList<>();

    @BeforeEach
    void clean() throws IOException {
        userDatabase.wipeUserDatabase();
        gameDatabase.wipeGameDatabase();
        for (WebSocketSession session : sessions) {
            session.close();
        }
        sessions.clear();
        userIDs.clear();
    }

    @AfterEach
    void cleanAfter() throws IOException {
        userDatabase.wipeUserDatabase();
        gameDatabase.wipeGameDatabase();
        for (WebSocketSession session : sessions) {
            session.close();
        }
        sessions.clear();
        userIDs.clear();
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Test
    public void testSaveGame() throws Exception {
        String lobbyID = setupLobby();
        startLobby(lobbyID);

        String response = mockMvc.perform(post("/api/game/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).isEqualTo("Game Saved");

        for (String u : userIDs) {
            List<String> saveIDs = gameDatabase.getSavedGames(u);
            assertThat(saveIDs).isNotEmpty();

            for (String saveID : saveIDs) {
                JsonNode snapshot = gameDatabase.getGameSnapshot(saveID);
                assertThat(snapshot).isNotNull();
                assertThat(snapshot.get("gameSnapshot"))
                        .isEqualTo(JsonUtil.parser("\"gameInfo\""));
            }
        }

    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Test
    public void testSeeSavedGames() throws Exception {
        String lobbyID = setupLobby();
        startLobby(lobbyID);
        saveGame(lobbyID);

        for (String uid : userIDs) {
            String response = seeSavedGames(uid);
            String saveID = gameDatabase.getSavedGames(uid).getFirst();
            assertThat(JsonUtil.parser(response)).containsExactly(JsonUtil.parser("{\"saveID\":\"" + saveID + "\"}"));
        }
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    @Test
    public void testLoadGame() throws Exception {
        String lobbyID = setupLobby();
        startLobby(lobbyID);

        Lobby lobBefore = serverManager.getLobbyFromLobbyID(lobbyID);
        Map<String, String> userToPlayerBefore = lobBefore.getUserToPlayer();
        Map<String, Client> playersBefore = lobBefore.getPlayers();

        saveGame(lobbyID);

        String uid1 = userIDs.getFirst();
        String saveID = getFirstSaveID(uid1);
        String loadedLobbyID = loadGame(uid1, saveID);

        Lobby lob = serverManager.getLobbyFromLobbyID(loadedLobbyID);

        assertThat(lob.getUserToPlayer()).isEqualTo(userToPlayerBefore);
        assertThat(lob.getPlayers()).containsExactly(Map.entry(uid1, serverManager.getClient(uid1)));
        assertThat(lob.getPlayerToUser()).isEmpty();

        loadPlayers(saveID);

        assertThat(lob.getUserToPlayer()).isEqualTo(userToPlayerBefore);
        assertThat(lob.getPlayers()).isEqualTo(playersBefore);
        assertThat(lob.getPlayerToUser()).isEmpty();
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    @Test
    public void testStartLoadedGameSuccessful() throws Exception {
        String lobbyID = setupLobby();
        startLobby(lobbyID);

        Lobby lobBefore = serverManager.getLobbyFromLobbyID(lobbyID);
        Map<String, String> playerToUserBefore = lobBefore.getPlayerToUser();

        saveGame(lobbyID);

        String uid1 = userIDs.getFirst();
        String saveID = getFirstSaveID(uid1);
        String loadedLobbyID = loadGame(uid1, saveID);

        loadPlayers(saveID);
        readyPlayers(loadedLobbyID);
        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", loadedLobbyID))))
                .andExpect(status().isOk());

        Lobby lob = serverManager.getLobbyFromLobbyID(loadedLobbyID);
        assertThat(lob.getPlayerToUser()).isEqualTo(playerToUserBefore);
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    @Test
    public void testStartLoadedGameFailed() throws Exception {
        String lobbyID = setupLobby();
        startLobby(lobbyID);

        saveGame(lobbyID);

        String uid1 = userIDs.getFirst();
        String saveID = getFirstSaveID(uid1);
        String loadedLobbyID = loadGame(uid1, saveID);
        readyPlayers(loadedLobbyID);
        String response = mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", loadedLobbyID))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).isEqualTo("Not all players have joined!");
    }


    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    private void startLobby(String lobbyID) throws Exception {
        readyPlayers(lobbyID);
        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());

    }
    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */

    private void readyPlayers(String lobbyID) throws Exception {
        for (String userID : userIDs) {
            Map<String, Object> readyBody1 = new HashMap<>();
            readyBody1.put("userID", userID);
            readyBody1.put("lobbyID", lobbyID);
            mockMvc.perform(post("/api/lobby/markReady")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(readyBody1)));
        }
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    private void saveGame(String lobbyID) throws Exception {
        mockMvc.perform(post("/api/game/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()));
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    private String getFirstSaveID(String userID) throws Exception {
        String response = mockMvc.perform(post("/api/game/seeSavedGames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("userID", userID))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonUtil.parser(response).get(0).get("saveID").asText();
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    private String loadGame(String userID, String saveID) throws Exception {
        return mockMvc.perform(post("/api/game/loadGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("userID", userID, "saveID", saveID))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    private void loadPlayers(String saveID) throws Exception {
        for (int i = 1; i < userIDs.size(); i++) {
            Map<String, Object> joinBody = Map.of(
                    "userID", userIDs.get(i),
                    "saveID", saveID
            );
            mockMvc.perform(post("/api/game/loadGame")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(joinBody)))
                    .andExpect(status().isCreated());
        }
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    private String seeSavedGames(String userID) throws Exception {
        return mockMvc.perform(post("/api/game/seeSavedGames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("userID", userID))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    private String setupLobby() throws Exception {
        String username = "TestUser";

        String token = createAndLoginUser(username, mapper, mockMvc);

        userIDs.add(token);

        Thread.sleep(50);

        WebSocketSession wsSession = connectWebSocket(token, port);
        Thread.sleep(50);
        sessions.add(wsSession);

        String lobbyID = LobbyRESTTests.createLobby(token, "testName","6",mapper,mockMvc);


        for (int i = 2; i <= 3; i++) {
            String usernameLoop = username + i;
            String tokenLoop = createAndLoginUser(usernameLoop, mapper, mockMvc);
            userIDs.add(tokenLoop);
            Thread.sleep(50);
            WebSocketSession wsSessionLoop = connectWebSocket(tokenLoop, port);
            Thread.sleep(50);
            sessions.add(wsSessionLoop);

            Map<String, Object> joinBody = new HashMap<>();
            joinBody.put("userID", tokenLoop);
            joinBody.put("lobbyID", lobbyID);
            mockMvc.perform(post("/api/lobby/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(joinBody)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(lobbyID));
        }
        return lobbyID;
    }
}
