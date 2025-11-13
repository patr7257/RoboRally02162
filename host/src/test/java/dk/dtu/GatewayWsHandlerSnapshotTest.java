package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.domain.core.*;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import dk.dtu.support.NoDelayPacer;
import dk.dtu.util.BoardTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * @author William Pii Jæger
 */
public class GatewayWsHandlerSnapshotTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private WebSocketSession session;
    private GameManager manager;
    private GatewaysWsHandler handler;
    private UUID gameId;
    private NoDelayPacer pacer;

    /**
     * @author William Pii Jæger
     */
    @BeforeEach
    void setup() {
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        Board board = BoardTestUtils.initEmptyBoard(3, 3);
        Robot r = new Robot(1, 1, 1, Direction.E);
        BoardAPI api = new BoardApiImpl(board, List.of(r));

        pacer = new NoDelayPacer();
        manager = new GameManager(pacer);
        gameId = manager.startGame(board, api, List.of(r));

        handler = new GatewaysWsHandler(manager);
        handler.afterConnectionEstablished(session);
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void submitMOVE1_thenRunRound_thenGetBoard_returnsSnapshotWithMovedRobot() throws Exception {
        handler.handleMessage(session, new TextMessage("""
            {"gameID":"%s","playerID":1,"payload":{"type":"startProgramming","windowMs":60000}}
        """.formatted(gameId)));

        handler.handleMessage(session, new TextMessage("""
            {"gameID":"%s","playerID":1,"payload":{"type":"submitProgram","cards":["MOVE1"]}}
        """.formatted(gameId)));

        GameSession s = manager.findSessionByID(gameId).orElseThrow();
        pacer.runAllRegisters(s);

        handler.handleMessage(session, new TextMessage("""
            {"gameID":"%s","playerID":1,"payload":{"type":"getBoard"}}
        """.formatted(gameId)));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeast(1)).sendMessage(sent.capture());

        TextMessage last = sent.getAllValues().stream()
                .filter(tm -> {
                    try {
                        JsonNode root = mapper.readTree(tm.getPayload());
                        return "stateSnapshot".equals(root.path("type").asText());
                    } catch (Exception e) { return false; }
                })
                .reduce((a,b) -> b)
                .orElseThrow();

        JsonNode root = mapper.readTree(last.getPayload());

        assertEquals("stateSnapshot", root.path("type").asText());
        assertEquals(gameId.toString(), root.path("meta").path("game").path("gameID").asText());

        JsonNode robot0 = root.path("payload").path("robots").get(0);
        assertEquals(2, robot0.path("x").asInt());
        assertEquals(1, robot0.path("y").asInt());
        assertEquals("E", robot0.path("facing").asText());
    }
}
