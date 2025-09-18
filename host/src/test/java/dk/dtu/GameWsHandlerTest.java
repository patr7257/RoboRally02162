package dk.dtu;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.domain.core.CommandResult;
import dk.dtu.domain.core.GameCommand;
import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.infrastructure.websocket.GatewayWsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Author(s) William Pii Jæger

class GameWsHandlerTest {

    private ObjectMapper mapper;
    private GameManager gameManager;
    private GatewayWsHandler handler;
    private WebSocketSession session;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        gameManager = mock(GameManager.class);
        handler = new GatewayWsHandler(gameManager);
        session = mock(WebSocketSession.class);

        when(session.isOpen()).thenReturn(true);
        when(gameManager.apply(any(UUID.class), any(GameCommand.class)))
                .thenReturn(CommandResult.ok("ok"));
    }

    @Test
    void submitPrograms_parsesPayloadAndCallsGameManager() throws Exception {
        UUID gameId = UUID.randomUUID();

        String json = """
                {
                  "gameID": "%s",
                  "playerID": 42,
                  "payload": { "type": "submitProgram", "cards": ["MOVE1","MOVE1"] }
                }
                """.formatted(gameId);

        handler.handleMessage(session, new TextMessage(json));

        ArgumentCaptor<GameCommand> cmdCap = ArgumentCaptor.forClass(GameCommand.class);
        verify(gameManager).apply(eq(gameId), cmdCap.capture());

        GameCommand.SubmitPrograms cmd = (GameCommand.SubmitPrograms) cmdCap.getValue();
        assert cmd.player().value() == 42;
        assert cmd.cards().equals(java.util.List.of(ProgramCard.move1(), ProgramCard.move1()));
    }
}
