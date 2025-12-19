package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.domain.core.GameManager;
import dk.dtu.infrastructure.dto.*;
import dk.dtu.infrastructure.websocket.GatewaysWsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Weihao Mo
 */
public class GatewaysWsHandlerTest {
    private GameManager gameManager;
    private GatewaysWsHandler handlerNew;
    private WebSocketSession session;
    private ObjectMapper mapper;

    /**
     * @author Weihao Mo
     */
    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        gameManager = mock(GameManager.class);
        handlerNew = new GatewaysWsHandler(gameManager);
        session = mock(WebSocketSession.class);

        when(session.isOpen()).thenReturn(true);
//        when(gameManager.apply(any(UUID.class), any(GameCommand.class)))
//                .thenReturn(CommandResult.ok("ok"));
        handlerNew.afterConnectionEstablished(session);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void testSendOutgoingMessageContent() throws Exception {
        UUID gameID = UUID.randomUUID();
        EventMetaDTO meta = new EventMetaDTO(new GameDto(gameID,null), new PlayerDto(1));
        OutgoingMessage<Object> msg = new OutgoingMessage<>(
                "stateSnapshot",
                Delivery.BROADCAST,
                meta,
                Map.of()
        );

        handlerNew.send(msg);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String sentPayload = captor.getValue().getPayload();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actual = mapper.readTree(sentPayload);
        JsonNode expected = mapper.valueToTree(msg);

        assertEquals(actual, expected);

    }


}
