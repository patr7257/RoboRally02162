package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import dk.dtu.util.BoardTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

// Author(s) William Pii Jæger

public class GatewayWsHandlerSnapshotTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private WebSocketSession session;
    private GameManager manager;
    private GatewaysWsHandler handler;

    private UUID gameId;

    @BeforeEach
    void setup() {
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        Board board = BoardTestUtils.initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);
        Robot r = new Robot(1, 1, 1, Direction.E);

        manager = new GameManager();
        gameId = manager.startGame(board, api, List.of(r));

        handler = new GatewaysWsHandler(manager);
    }

    @Test
    void submitMOVE1_thenStartRound_broadcastsSnapshotWithMovedRobot() throws Exception {
        // This makes absolutely sure that websocket actually works and is
        // hooked up correctly with GameManager
        // We check that the packages received are correct
        String submitJson = """
                {
                  "gameID": "%s",
                  "playerID": 1,
                  "payload": { "type": "submitProgram", "cards": ["MOVE1"] }
                }
                """.formatted(gameId);
        handler.handleMessage(session, new TextMessage(submitJson));

        String startRoundJson = """
        {
          "gameID": "%s",
          "playerID": 1,
          "payload": { "type": "startRound" }
        }
        """.formatted(gameId);

        handler.handleMessage(session, new TextMessage(startRoundJson));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeast(1)).sendMessage(sent.capture());

        TextMessage lastFrame = sent.getAllValues().get(sent.getAllValues().size()-1);
        JsonNode root = mapper.readTree(lastFrame.getPayload());

        assertEquals("stateSnapshot", root.path("type").asText());
        assertEquals("BROADCAST", root.path("delivery").asText());
        assertEquals(gameId.toString(), root.path("meta").path("game").path("gameID").asText());
        assertEquals(1, root.path("meta").path("player").path("playerID").asInt());

        JsonNode robots = root.path("payload").path("robots");
        assertTrue(robots.isArray() && robots.size() == 1);

        JsonNode robot0 = robots.get(0);

        assertEquals(2, robot0.path("x").asInt());
        assertEquals(1, robot0.path("y").asInt());
        assertEquals("E", robot0.path("facing").asText());

        assertTrue(root.path("payload").path("board").has("width"));
        assertTrue(root.path("payload").path("board").has("height"));
        assertTrue(root.path("payload").path("board").has("cells") || root.path("payload").path("board").has("tiles"));
    }
}
