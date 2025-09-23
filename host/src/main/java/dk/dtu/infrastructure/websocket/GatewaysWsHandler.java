package dk.dtu.infrastructure.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.domain.core.*;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Author(s) Weihao Mo, William Pii Jæger


public class GatewaysWsHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final GameManager gameManager;
    private WebSocketSession session;

    public GatewaysWsHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
    }


    public void send(OutgoingMessage outgoingMessage) throws IOException {
        if (session != null && session.isOpen()) {
            String json = mapper.writeValueAsString(outgoingMessage);
            session.sendMessage(new TextMessage(json));
        } else {
            throw new IllegalArgumentException("No open WebSocket session to gateway");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        GatewaysWsHandler.IncomingMessage msg = mapper.readValue(message.getPayload(), GatewaysWsHandler.IncomingMessage.class);
        UUID gameID = UUID.fromString(msg.gameId);
        PlayerDto player = new PlayerDto(msg.playerId);
        GameDto game = new GameDto(gameID);
        EventMetaDTO meta = new EventMetaDTO(game, player);

        GameCommand cmd = msg.toDomainCommand(mapper);
        CommandResult result = gameManager.apply(UUID.fromString(msg.gameId), cmd);

        Optional<Game> maybeGame = gameManager.findByID(gameID);
        if (maybeGame.isPresent()) {
            Game g = maybeGame.get();
            BoardDto board = SnapshotMapper.toBoardDto(g.getBoard());
            List<RobotDto> robots = SnapshotMapper.mapRobots(g.getRobots());

            SnapshotPayload payload = new SnapshotPayload(board, robots);
            OutgoingMessage out = new OutgoingMessage<>("stateSnapshot", Delivery.BROADCAST, meta, payload);

            session.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
        } else {
            OutgoingMessage out = new OutgoingMessage<>(
                    "error",
                    dk.dtu.infrastructure.dto.Delivery.DIRECT,
                    meta,
                    java.util.Map.of("message", "Game not found")
            );
            session.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IncomingMessage(
            @JsonProperty("gameID") String gameId,
            @JsonProperty("playerID") int playerId,
            JsonNode payload
    ) {
        GameCommand toDomainCommand(ObjectMapper mapper) throws JsonProcessingException {
            String t = payload != null && payload.hasNonNull("type")
                    ? payload.get("type").asText()
                    : null;
            if (t == null || t.isBlank())
                throw new IllegalArgumentException("payload.type is required");

            return switch (t) {
                case "startRound" -> new GameCommand.StartRound();

                case "submitCards", "submitProgram" -> {
                    SubmitCardsPayload pl = mapper.treeToValue(payload, SubmitCardsPayload.class);
                    yield new GameCommand.SubmitPrograms(new PlayerID(playerId),
                            pl.revertStringToCard(pl.cards()));
                }

                case "endGame" -> new GameCommand.EndGame();

                default -> throw new IllegalArgumentException("Unknown payload.type: " + t);
            };
        }
    }


    public boolean isConnected() {
        return session != null && session.isOpen();
    }
}
