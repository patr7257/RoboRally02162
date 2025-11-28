
package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.User;
import dk.dtu.model.database.DynamicGameDatabase;
import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.shared.GameService;
import dk.dtu.shared.ServerManager;
import dk.dtu.util.JsonUtil;
import dk.dtu.util.TokenUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

import static dk.dtu.LobbyRESTTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(HostConfig.class)
@AutoConfigureMockMvc
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

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private GameService gameService;

    @LocalServerPort
    int port;

    private final List<WebSocketSession> sessions = new ArrayList<>();
    private final List<String> userTokens = new ArrayList<>();

    @BeforeEach
    void clean() throws IOException {
        gameDatabase.wipeGameDatabase();
        userDatabase.wipeUserDatabase();
        for (WebSocketSession session : sessions) {
            session.close();
        }
        sessions.clear();
        userTokens.clear();
    }

    @AfterEach
    void cleanAfter() throws IOException {
        gameDatabase.wipeGameDatabase();
        userDatabase.wipeUserDatabase();
        for (WebSocketSession session : sessions) {
            session.close();
        }
        sessions.clear();
        userTokens.clear();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @Test
    public void testSaveGame() throws Exception {
        String lobbyID = setupLobby();
        String firstUserToken = userTokens.getFirst();
        startLobby(firstUserToken, lobbyID);

        saveGame(lobbyID);

        for (String token : userTokens) {
            String userID = tokenUtil.extractUserToken(token).userID();
            List<String> saveIDs = gameDatabase.getSavedGames(userID);
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
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    public void testDeleteSavedGame() throws Exception {
        String lobbyID = setupLobby();
        String firstUserToken = userTokens.getFirst();
        startLobby(firstUserToken, lobbyID);
        saveGame(lobbyID);

        String saveID = getFirstSaveID(firstUserToken);

        String newToken = createAndLoginUser("Benjamin", mapper, mockMvc);
        Thread.sleep(50);

        WebSocketSession wsSession = connectWebSocket(newToken, port);
        Thread.sleep(50);
        sessions.add(wsSession);

        mockMvc.perform(post("/api/game/deleteSavedGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + newToken)
                        .content(mapper.writeValueAsString(Map.of("saveID", saveID))))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User not in game"));

        mockMvc.perform(post("/api/game/deleteSavedGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstUserToken)
                        .content(mapper.writeValueAsString(Map.of("saveID", saveID))))
                .andExpect(status().isOk())
                .andExpect(content().string("Game Deleted"));

        for (String token : userTokens) {
            String uid = tokenUtil.extractUserToken(token).userID();
            List<String> saves = gameDatabase.getSavedGames(uid);
            assertThat(saves).doesNotContain(saveID);
        }

        assertThat(gameDatabase.getGameSnapshot(saveID)).isNull();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @Test
    public void testSeeSavedGames() throws Exception {
        String lobbyID = setupLobby();
        String firstUserToken = userTokens.getFirst();
        startLobby(firstUserToken, lobbyID);
        saveGame(lobbyID);

        for (String token : userTokens) {
            String userID = tokenUtil.extractUserToken(token).userID();
            String response = seeSavedGames(token);
            String saveID = gameDatabase.getSavedGames(userID).getFirst();
            assertThat(JsonUtil.parser(response)).containsExactly(JsonUtil.parser("{\"saveID\":\"" + saveID + "\"}"));
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @Test
    public void testLoadGame() throws Exception {
        String lobbyID = setupLobby();
        String firstUserToken = userTokens.getFirst();
        startLobby(firstUserToken, lobbyID);

        Lobby lobBefore = serverManager.getLobbyFromLobbyID(lobbyID);
        Map<String, String> userToPlayerBefore = lobBefore.getUserToPlayer();
        Map<String, Client> playersBefore = lobBefore.getPlayers();

        saveGame(lobbyID);

        String userID = tokenUtil.extractUserToken(firstUserToken).userID();
        String saveID = getFirstSaveID(firstUserToken);
        String loadedLobbyID = loadGame(firstUserToken, saveID);

        Lobby lob = serverManager.getLobbyFromLobbyID(loadedLobbyID);

        assertThat(lob.getUserToPlayer()).isEqualTo(userToPlayerBefore);
        assertThat(lob.getPlayers()).containsExactly(Map.entry(userID, serverManager.getClient(userID)));
        assertThat(lob.getPlayerToUser()).isEmpty();

        loadPlayers(saveID);

        assertThat(lob.getUserToPlayer()).isEqualTo(userToPlayerBefore);
        assertThat(lob.getPlayers()).isEqualTo(playersBefore);
        assertThat(lob.getPlayerToUser()).isEmpty();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @Test
    public void testStartLoadedGameSuccessful() throws Exception {
        String lobbyID = setupLobby();
        String firstUserToken = userTokens.getFirst();
        startLobby(firstUserToken, lobbyID);

        Lobby lobBefore = serverManager.getLobbyFromLobbyID(lobbyID);
        Map<String, String> playerToUserBefore = lobBefore.getPlayerToUser();

        saveGame(lobbyID);

        String saveID = getFirstSaveID(firstUserToken);
        String loadedLobbyID = loadGame(firstUserToken, saveID);

        loadPlayers(saveID);
        readyPlayers(loadedLobbyID);
        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstUserToken)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", loadedLobbyID))))
                .andExpect(status().isOk());

        Lobby lob = serverManager.getLobbyFromLobbyID(loadedLobbyID);
        assertThat(lob.getPlayerToUser()).isEqualTo(playerToUserBefore);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @Test
    public void testStartLoadedGameFailed() throws Exception {
        String lobbyID = setupLobby();
        String firstUserToken = userTokens.getFirst();
        startLobby(firstUserToken, lobbyID);

        saveGame(lobbyID);

        String saveID = getFirstSaveID(firstUserToken);
        String loadedLobbyID = loadGame(firstUserToken, saveID);
        readyPlayers(loadedLobbyID);
        String response = mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstUserToken)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", loadedLobbyID))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).isEqualTo("Not all players have joined!");
    }


    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private void startLobby(String userToken, String lobbyID) throws Exception {
        readyPlayers(lobbyID);
        mockMvc.perform(post("/api/lobby/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(mapper.writeValueAsString(Map.of("lobbyID", lobbyID))))
                .andExpect(status().isOk());

    }
    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private void readyPlayers(String lobbyID) throws Exception {
        for (String token : userTokens) {
            Map<String, Object> readyBody1 = new HashMap<>();
            readyBody1.put("lobbyID", lobbyID);
            mockMvc.perform(post("/api/lobby/markReady")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .content(mapper.writeValueAsString(readyBody1)));
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private void saveGame(String lobbyID) {
        Lobby lob = serverManager.getLobbyFromLobbyID(lobbyID);
        gameService.saveGame(lob);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private String getFirstSaveID(String userToken) throws Exception {
        String response = mockMvc.perform(get("/api/game/seeSavedGames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonUtil.parser(response).get(0).get("saveID").asText();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private String loadGame(String userToken, String saveID) throws Exception {
        return mockMvc.perform(post("/api/game/loadGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(mapper.writeValueAsString(Map.of("saveID", saveID))))
                .andExpect(status().isCreated())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private void loadPlayers(String saveID) throws Exception {
        for (int i = 1; i < userTokens.size(); i++) {
            Map<String, Object> joinBody = Map.of(
                    "saveID", saveID
            );
            mockMvc.perform(post("/api/game/loadGame")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + userTokens.get(i))
                            .content(mapper.writeValueAsString(joinBody)))
                    .andExpect(status().isCreated());
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private String seeSavedGames(String userToken) throws Exception {
        return mockMvc.perform(get("/api/game/seeSavedGames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    private String setupLobby() throws Exception {
        String username = "TestUser";

        String token = createAndLoginUser(username, mapper, mockMvc);

        userTokens.add(token);

        Thread.sleep(50);

        WebSocketSession wsSession = connectWebSocket(token, port);
        Thread.sleep(50);
        sessions.add(wsSession);

        String lobbyID = createLobby(token, "testName","6",mapper,mockMvc);

        for (int i = 2; i <= 3; i++) {
            String usernameLoop = username + i;
            String tokenLoop = createAndLoginUser(usernameLoop, mapper, mockMvc);
            userTokens.add(tokenLoop);
            Thread.sleep(50);
            WebSocketSession wsSessionLoop = connectWebSocket(tokenLoop, port);
            Thread.sleep(50);
            sessions.add(wsSessionLoop);

            Map<String, Object> joinBody = new HashMap<>();
            joinBody.put("lobbyID", lobbyID);
            mockMvc.perform(post("/api/lobby/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tokenLoop)
                            .content(mapper.writeValueAsString(joinBody)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(lobbyID));
        }
        return lobbyID;
    }
}
