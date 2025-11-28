package dk.dtu.infrastructure.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.domain.core.*;
import dk.dtu.domain.core.reaction.*;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 */
public class GatewaysWsHandler extends TextWebSocketHandler implements GameManagerObserver {
    private final ObjectMapper mapper = new ObjectMapper();
    private final GameManager gameManager;
    private WebSocketSession session;

    /**
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    public GatewaysWsHandler(GameManager gameManager) {
        this.gameManager = gameManager;
        gameManager.addObserver(this);
    }

    /**
     * Overwridden and sets the session to the provided session
     *
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
    }

    /**
     * Sends an outgoing message via the WebSocket session.
     *
     * @param msg message to send
     * @throws IOException           if JSON serialization or WebSocket send fails
     * @throws IllegalStateException if no open WebSocket session is available
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    public synchronized void send(OutgoingMessage<?> msg) throws IOException {
        WebSocketSession s = this.session;
        if (s == null || !s.isOpen()) throw new IllegalStateException("No open WebSocket session");
        String json = mapper.writeValueAsString(msg);
        s.sendMessage(new TextMessage(json));
    }

    /**
     * @author William Pii Jæger
     */
    private void sendError(String errorType, String message, EventMetaDTO meta, UUID gameId) {
        try {
            if (meta == null) {
                meta = new EventMetaDTO(new GameDto(gameId, null), null);
            }
            OutgoingMessage<?> out = new OutgoingMessage<>(
                    "error",
                    Delivery.DIRECT,
                    meta,
                    Map.of("type", errorType, "message", message)
            );
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to send error message: " + e);
        }
    }

    /**
     * @author William Pii Jæger
     */
    private UUID safeUUID(String str) {
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @author William Pii Jæger
     */
    private long readLongOrDefault(JsonNode node, String field, long defaultValue) {
        if (node.has(field)) {
            return node.get(field).asLong(defaultValue);
        }
        return defaultValue;
    }

    /**
     * @author William Pii Jæger
     */
    private EventMetaDTO withGame(EventMetaDTO meta, GameDto game) {
        return new EventMetaDTO(game, meta.player());
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String raw = message.getPayload();
        final IncomingMessage msg;

        try {
            msg = mapper.readValue(raw, IncomingMessage.class);
        } catch (Exception e) {
            sendError("bad_request", "Invalid JSON", null, null);
            return;
        }

        if (msg.payload == null || !msg.payload.hasNonNull("type")) {
            sendError("bad_request", "payload.type is required", null, null);
            return;
        }

        final String type = msg.payload.get("type").asText();
        final UUID gameId = safeUUID(msg.gameId);

        if (gameId == null) {
            sendError("bad_request", "Invalid gameID", null, null);
            return;
        }

        final PlayerDto playerDto = new PlayerDto(msg.playerId);
        final EventMetaDTO meta = new EventMetaDTO(new GameDto(gameId, null), playerDto);

        switch (type) {
            case "startProgramming" -> {
                long windowMs = readLongOrDefault(msg.payload, "windowMs", 60_000L);
                GameCommand cmd = new GameCommand.StartProgramming(UUID.randomUUID(), gameId, windowMs);
                CommandResult result = gameManager.execute(cmd);

                if (result.ok()) {
                    send(new OutgoingMessage<>("ack", Delivery.DIRECT, meta,
                            Map.of("message", "Programming phase started")));
                } else {
                    sendError("command_failed", result.reason(), meta, gameId);
                }
            }

            case "submitProgram", "submitCards" -> {
                try {
                    SubmitCardsPayload pl = mapper.treeToValue(msg.payload, SubmitCardsPayload.class);
                    List<ProgramCard> cards = pl.revertStringToCard(pl.cards());
                    GameCommand cmd = new GameCommand.SubmitPrograms(
                            UUID.randomUUID(),
                            gameId,
                            new PlayerID(msg.playerId),
                            cards
                    );
                    CommandResult result = gameManager.execute(cmd);

                    if (result.ok()) {
                        send(new OutgoingMessage<>("ack", Delivery.DIRECT, meta,
                                Map.of("message", "Program submitted")));
                    } else {
                        sendError("command_failed", result.reason(), meta, gameId);
                    }
                } catch (Exception e) {
                    sendError("bad_request", "Invalid cards payload", meta, gameId);
                }
            }

            case "endGame" -> {
                GameCommand cmd = new GameCommand.EndGame(UUID.randomUUID(), gameId);
                CommandResult result = gameManager.execute(cmd);

                if (result.ok()) {
                    send(new OutgoingMessage<>("ack", Delivery.DIRECT, meta,
                            Map.of("message", "Game ended")));
                } else {
                    sendError("command_failed", result.reason(), meta, gameId);
                }
            }

            case "getBoard" -> {
                Optional<SnapshotPayload> snapOpt = gameManager.query(gameId, new GameQuery.GetSnapshot());
                if (snapOpt.isEmpty()) {
                    sendError("not_found", "Game not found", meta, gameId);
                    return;
                }
                SnapshotPayload payload = snapOpt.get();
                send(new OutgoingMessage<>("stateSnapshot", Delivery.DIRECT,
                        withGame(meta, payload.game()), payload));
            }

            case "getHand" -> {
                Optional<List<ProgramCard>> cardsOpt = gameManager.query(
                        gameId,
                        new GameQuery.GetHand(msg.playerId)
                );
                if (cardsOpt.isEmpty()) {
                    sendError("not_found", "Game not found", meta, gameId);
                    return;
                }

                List<String> cardStrings = cardsOpt.get().stream()
                        .map(pc -> pc.toString().equals("MOVE-1") ? "MOVEBACK" : pc.toString())
                        .collect(Collectors.toList());

                Optional<Game> gameOpt = gameManager.findByID(gameId);
                GameDto gameDto = gameOpt.map(g -> SnapshotMapper.mapGame(gameId, g))
                        .orElse(new GameDto(gameId, null));

                HandPayload payload = new HandPayload("hand", cardStrings);
                send(new OutgoingMessage<>("hand", Delivery.DIRECT,
                        withGame(meta, gameDto), payload));
            }

            case "getDiscard" -> {
                Optional<List<ProgramCard>> cardsOpt = gameManager.query(
                        gameId,
                        new GameQuery.GetDiscard(msg.playerId)
                );

                List<String> cardStrings = cardsOpt.get().stream()
                        .map(pc -> pc.toString().equals("MOVE-1") ? "MOVEBACK" : pc.toString())
                        .collect(Collectors.toList());

                Optional<Game> gameOpt = gameManager.findByID(gameId);
                GameDto gameDto = gameOpt.map(g -> SnapshotMapper.mapGame(gameId, g))
                        .orElse(new GameDto(gameId, null));

                DiscardPayload payload = new DiscardPayload("discard", cardStrings);
                send(new OutgoingMessage<>("discard", Delivery.DIRECT,
                        withGame(meta, gameDto), payload));
            }

            case "getReadiness" -> {
                Optional<ReadinessDto> rdOpt = gameManager.query(gameId, new GameQuery.GetReadiness());
                if (rdOpt.isEmpty()) {
                    sendError("not_found", "Game not found", meta, gameId);
                    return;
                }
                ReadinessDto rd = rdOpt.get();
                send(new OutgoingMessage<>("readiness", Delivery.DIRECT, meta, rd));
            }

            case "getTimeRemaining" -> {
                Optional<Long> timeOpt = gameManager.query(gameId, new GameQuery.GetTimeRemaining());
                if (timeOpt.isEmpty()) {
                    sendError("not_found", "Game not found", meta, gameId);
                    return;
                }
                send(new OutgoingMessage<>("timeRemaining", Delivery.DIRECT, meta,
                        Map.of("ms", timeOpt.get())));
            }

            case "setRespawnDirection" -> {
                try {
                    String directionStr = msg.payload.get("direction").asText();

                    if (directionStr == null || directionStr.isEmpty()) {
                        sendError("bad_request", "Direction is required", meta, gameId);
                        return;
                    }

                    Direction direction;
                    try {
                        direction = Direction.valueOf(directionStr);
                    } catch (IllegalArgumentException e) {
                        sendError("bad_request", "Invalid direction: " + directionStr +
                                ". Must be N, S, E, or W", meta, gameId);
                        return;
                    }

                    GameCommand cmd = new GameCommand.SetRespawnDirection(
                            UUID.randomUUID(), gameId, new PlayerID(msg.playerId), direction);
                    CommandResult result = gameManager.execute(cmd);

                    if (result.ok()) {
                        send(new OutgoingMessage<>("ack", Delivery.DIRECT, meta,
                                Map.of("message", "Respawn direction set to " + direction)));
                    } else {
                        sendError("command_failed", result.reason(), meta, gameId);
                    }
                } catch (Exception e) {
                    sendError("bad_request",
                            "Failed to set respawn direction: " + e.getMessage(),
                            meta, gameId);
                }
            }

            case "submitReaction" -> {
                try {
                    String choiceStr = msg.payload.get("choice").asText();

                    if (choiceStr == null) {
                        sendError("bad_request", "choice is required", meta, gameId);
                        return;
                    }

                    ReactionChoice choice = parseReactionChoice(choiceStr);

                    if (choice == null) {
                        sendError("bad_request", "Invalid choice: " + choiceStr, meta, gameId);
                        return;
                    }

                    Optional<GameSession> sessionOpt = gameManager.findSessionByID(gameId);
                    if (sessionOpt.isEmpty()) {
                        sendError("not_found", "Game not found", meta, gameId);
                        return;
                    }

                    GameSession gs = sessionOpt.get();
                    ReactionRequest<?> pending = gs.getPendingReaction();
                    if (pending == null) {
                        sendError("command_failed", "No pending reaction", meta, gameId);
                        return;
                    }

                    GameCommand cmd = new GameCommand.SubmitReaction(
                            UUID.randomUUID(),
                            gameId,
                            new PlayerID(msg.playerId),
                            pending.id(),
                            choice
                    );
                    CommandResult result = gameManager.execute(cmd);

                    if (result.ok()) {
                        send(new OutgoingMessage<>("ack", Delivery.DIRECT, meta,
                                Map.of("message", "Reaction submitted")));
                    } else {
                        sendError("command_failed", result.reason(), meta, gameId);
                    }
                } catch (Exception e) {
                    sendError("bad_request",
                            "Failed to submit reaction: " + e.getMessage(),
                            meta, gameId);
                }
            }

            default -> {
                sendError("bad_request", "Unknown payload.type: " + type, meta, gameId);
            }
        }
    }

    /**
     * Parses a reaction choice string into the appropriate ReactionChoice type.
     *
     * @author William Pii Jæger
     */
    private ReactionChoice parseReactionChoice(String choice) {
        try {
            return ReactionChoice.SandBoxChoice.valueOf(choice);
        } catch (IllegalArgumentException e1) {
            try {
                return ReactionChoice.WeaselChoice.valueOf(choice);
            } catch (IllegalArgumentException e2) {
                try {
                    return ReactionChoice.SpeedChoice.valueOf(choice);
                } catch (IllegalArgumentException e3) {
                    return null;
                }
            }
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author William Pii Jæger
     * @author Niklas Emil Lysdal
     */
    @Override
    public void handleGameUpdate(Game game, UUID gameID) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            EventMetaDTO meta = new EventMetaDTO(gameDto, null);
            UpdatePayload payload = new UpdatePayload("update");
            OutgoingMessage<?> out = new OutgoingMessage<>("update", Delivery.BROADCAST, meta, payload);
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast game update: " + e);
        }
    }

    /**
     * @author William Pii Jæger
     */
    @Override
    public void onProgrammingStarted(Game game, UUID gameID) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            EventMetaDTO meta = new EventMetaDTO(gameDto, null);
            OutgoingMessage<?> out = new OutgoingMessage<>("programmingStarted", Delivery.BROADCAST, meta,
                    Map.of("message", "Programming phase started"));
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast programming started: " + e);
        }
    }

    /**
     * @author William Pii Jæger
     */
    @Override
    public void onPlayerSubmitted(Game game, UUID gameID, PlayerID playerId) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            EventMetaDTO meta = new EventMetaDTO(gameDto, null);
            OutgoingMessage<?> out = new OutgoingMessage<>("playerSubmitted", Delivery.BROADCAST, meta,
                    Map.of("playerId", playerId.value()));
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast player submitted: " + e);
        }
    }

    /**
     * @author William Pii Jæger
     */
    @Override
    public void onRoundExecuting(Game game, UUID gameID) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            EventMetaDTO meta = new EventMetaDTO(gameDto, null);
            OutgoingMessage<?> out = new OutgoingMessage<>("roundExecuting", Delivery.BROADCAST, meta,
                    Map.of("message", "Round is executing"));
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast round executing: " + e);
        }
    }

    /**
     * Handles the event when a game has finished by broadcasting the result to all clients.
     * Creates a GameDto snapshot of the final game state and sends it with winner information.
     *
     * @param game the finished game instance
     * @param gameID id of the game
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    @Override
    public void onGameFinished(Game game, UUID gameID) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            EventMetaDTO meta = new EventMetaDTO(gameDto, null);
            OutgoingMessage<?> out = new OutgoingMessage<>("gameFinished", Delivery.BROADCAST, meta,
                    Map.of("winner", game.getWinner().map(PlayerID::value).orElse(null)));
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast game finished: " + e);
        }
    }

    /**
     * Notifies clients when a robot needs to choose a respawn direction.
     *
     * @param game the game instance
     * @param gameID the game ID
     * @param robotId the ID of the robot that died
     *
     * @author Weihao Mo
     */
    @Override
    public void onRobotNeedsRespawn(Game game, UUID gameID, int robotId) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            EventMetaDTO meta = new EventMetaDTO(gameDto, null);

            OutgoingMessage<?> out = new OutgoingMessage<>("needRespawnDirection", Delivery.BROADCAST, meta,
                    Map.of("robotId", robotId));
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast robot needs respawn: " + e);
        }
    }

    /**
     * Notifies the specific player that they need to make a reaction choice.
     *
     * @param game the game instance
     * @param gameID the game ID
     * @param request the reaction request with details about the choice needed
     *
     * @author William Pii Jæger
     */
    @Override
    public void onReactionNeeded(Game game, UUID gameID, ReactionRequest<?> request) {
        try {
            GameDto gameDto = SnapshotMapper.mapGame(gameID, game);
            int playerId = Integer.parseInt(request.robotid());
            PlayerDto playerDto = new PlayerDto(playerId);
            EventMetaDTO meta = new EventMetaDTO(gameDto, playerDto);

            Map<String, Object> payload = new HashMap<>();
            payload.put("kind", request.spec().kind().toString());
            payload.put("options", request.spec().options().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
            payload.put("deadline", request.deadline().toEpochMilli());

            OutgoingMessage<?> out = new OutgoingMessage<>("reactionNeeded", Delivery.DIRECT, meta, payload);
            send(out);
        } catch (Exception e) {
            System.err.println("Failed to broadcast reaction needed: " + e);
        }
    }

    /**
     * An incoming message from a client containing game-related information.
     *
     * @param gameId the id of the game
     * @param playerId the id of the player
     * @param payload the message payload containing JsonNode
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record IncomingMessage(
            @JsonProperty("gameID") String gameId,
            @JsonProperty("playerID") int playerId,
            JsonNode payload
    ) {

    }

    /**
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }
}