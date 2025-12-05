package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.ReactionRequest;
import dk.dtu.domain.core.reaction.ReactionResolution;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.DamageDecks;
import dk.dtu.domain.model.Deck;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.DamageDecksDto;
import dk.dtu.infrastructure.dto.ReadinessDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;

import java.util.*;

/**
 * Coordinates active game sessions, forwards commands to sessions/games,
 * and bridges scheduler (RoundPacer) events to external observers.
 * Implements {@link GameObserver} to fan out game updates to {@link GameManagerObserver}s.
 *
 * @author William Pii Jæger
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */
public class GameManager implements GameObserver {
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final List<GameManagerObserver> observers = new ArrayList<>();
    private final RoundPacer pacer;

    /**
     * Creates a manager and registers an internal bridge listener on the provided pacer.
     *
     * @param pacer the round pacer used for scheduling programming/execution phases
     * @author William Pii Jæger
     */
    public GameManager(RoundPacer pacer) {
        this.pacer = pacer;
        this.pacer.addListener(new PacerBridge());
    }

    /**
     * Starts a new game with the given board, API, and robots. Registers this as a {@link GameObserver}.
     *
     * @param board   the game board
     * @param api     board API for rule application
     * @param players participating robots
     * @return the UUID of the newly created game session
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public UUID startGame(Board board, BoardAPI api, List<Robot> players) {
        UUID id = UUID.randomUUID();
        Game game = new Game(board, api, players);
        game.addObserver(this);
        GameSession session = new GameSession(id, game);
        activeSessions.put(id, session);
        return id;
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     * @author Weihao Mo
     */
    public UUID startGame(Board board, BoardAPI api, List<Robot> players, Map<Integer, Deck> decks, DamageDecks damageDecks) {
        UUID id = UUID.randomUUID();
        Game game = new Game(board, api, players, decks,damageDecks);
        game.addObserver(this);
        GameSession session = new GameSession(id, game);
        activeSessions.put(id, session);
        return id;
    }


    /**
     * Gracefully ends an existing game: cancels scheduled tasks and marks session finished.
     *
     * @param id the game ID
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public void endGame(UUID id) {
        GameSession session = activeSessions.remove(id);
        if (session != null) {
            session.cancelAutoExecuteTask();
            session.cancelStepTask();
            session.setState(GameState.FINISHED);
        }
    }

    /**
     * Finds a session by ID.
     *
     * @param gameID the game ID
     * @return optional session if present
     * @author William Pii Jæger
     */
    public Optional<GameSession> findSessionByID(UUID gameID) {
        return Optional.ofNullable(activeSessions.get(gameID));
    }

    /**
     * Finds a game by ID.
     *
     * @param gameID the game ID
     * @return optional game if present
     * @author William Pii Jæger
     */
    public Optional<Game> findByID(UUID gameID) {
        return findSessionByID(gameID).map(GameSession::getGame);
    }

    /**
     * Executes a game command against the addressed session, guarding session lifecycle/state.
     *
     * @param cmd the command to execute
     * @return result describing success/failure and message
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public synchronized CommandResult execute(GameCommand cmd) {
        return switch (cmd) {
            case GameCommand.StartProgramming start -> {
                GameSession session = activeSessions.get(start.gameId());
                if (session == null) yield CommandResult.fail("Game not found");
                if (session.getState() != GameState.WAITING_TO_START &&
                        session.getState() != GameState.FINISHED) {
                    yield CommandResult.fail("Game already in progress");
                }
                try {
                    pacer.scheduleProgrammingPhase(session, start.windowMs());
                    yield CommandResult.ok("Programming phase started");
                } catch (Exception e) {
                    yield CommandResult.fail("Failed to start programming: " + e.getMessage());
                }
            }
            case GameCommand.SubmitPrograms submit -> {
                GameSession session = activeSessions.get(submit.gameId());
                if (session == null) yield CommandResult.fail("Game not found");
                if (session.getState() != GameState.PROGRAMMING) yield CommandResult.fail("Not in programming phase");
                if (session.hasSubmitted(submit.player())) yield CommandResult.fail("Already submitted for this round");

                try {
                    // potential session lock could be here?
                    synchronized (session) {
                        Game game = session.getGame();
                        game.submitProgram(submit.player(), submit.cards());
                    }
                    // if lock, we should notify pacer outside the lock
                    pacer.onPlayerSubmitted(session, submit.player());
                    yield CommandResult.ok("Program submitted");
                } catch (Exception e) {
                    yield CommandResult.fail("SubmitPrograms failed: " + e.getMessage());
                }
            }
            case GameCommand.EndGame end -> {
                GameSession session = activeSessions.remove(end.gameId());
                if (session != null) {
                    session.cancelAutoExecuteTask();
                    session.cancelStepTask();
                    session.setState(GameState.FINISHED);
                }
                yield CommandResult.ok("Game ended");
            }

            case GameCommand.SetRespawnDirection setRespawnDirection -> {
                GameSession session = activeSessions.get(setRespawnDirection.gameId());
                if(session == null) yield CommandResult.fail("Game not found");

                try {
                    synchronized (session) {
                        Game game = session.getGame();
                        Robot robot = game.getRobot(setRespawnDirection.player());

                        game.setRespawnDirection(setRespawnDirection.player(), setRespawnDirection.direction());
                        session.markRespawnDirectionSet(robot.getId());

                        if (session.allRespawnDirectionsSet()) {
                            if (pacer instanceof GameScheduler scheduler) {
                                scheduler.continueAfterAllRespawns(session);
                            }
                        }
                    }
                    yield CommandResult.ok("Respawn direction set to " + setRespawnDirection.direction());
                } catch (Exception e) {
                    yield CommandResult.fail("Failed to set respawn direction: " + e.getMessage());
                }
            }

            case GameCommand.SubmitReaction submitReaction -> {
                GameSession session = activeSessions.get(submitReaction.gameId());
                if (session == null) yield CommandResult.fail("Game not found");

                try {
                    synchronized (session) {
                        ReactionRequest<?> pending = session.getPendingReaction();
                        if (pending == null) {
                            yield CommandResult.fail("No pending reaction");
                        }
                        if (!pending.id().equals(submitReaction.reactionId())) {
                            yield CommandResult.fail("Reaction ID mismatch");
                        }
                        if (!String.valueOf(submitReaction.player().value()).equals(pending.robotid())) {
                            yield CommandResult.fail("Wrong player for this reaction");
                        }

                        ReactionResolution<?> resolution = new ReactionResolution<>(
                                submitReaction.reactionId(),
                                submitReaction.choice()
                        );

                        if (pacer instanceof GameScheduler scheduler) {
                            scheduler.onReactionSubmitted(session, resolution);
                        }
                    }
                    yield CommandResult.ok("Reaction submitted");
                } catch (Exception e) {
                    yield CommandResult.fail("Failed to submit reaction: " + e.getMessage());
                }
            }
        };
    }

    /**
     * Queries a session for data (snapshot, hand, readiness, time remaining).
     * Uses an unsafe cast pattern internal to this layer but safe by contract.
     *
     * @param gameId the game ID
     * @param query  the query discriminator
     * @param <T>    response type corresponding to the query
     * @return optional result if session exists
     * @author William Pii Jæger
     * @author Weihao Mo
     * @author Benjamin Benyo Endahl Hansen
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    public synchronized <T> Optional<T> query(UUID gameId, GameQuery<T> query) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) return Optional.empty();

        synchronized (session) {
            return switch (query) {
                case GameQuery.GetSnapshot snap -> {
                    Game game = session.getGame();
                    SnapshotPayload payload = SnapshotMapper.createSnapshot(gameId, game);
                    yield Optional.of((T) payload);
                }
                case GameQuery.GetHand hand -> {
                    Game game = session.getGame();
                    List<ProgramCard> cards = game.getRobotHand(hand.robotId());
                    yield Optional.of((T) cards);
                }
                case GameQuery.GetDiscard discard -> {
                    Game game = session.getGame();
                    List<ProgramCard> cards = game.getRobotDiscard(discard.robotId());
                    yield Optional.of((T) cards);
                }

                case GameQuery.GetLastMove lastMove -> {
                    Game game = session.getGame();
                    Map<Integer,String> card = game.getLastMove();
                    yield Optional.ofNullable((T) card);
                }

                case GameQuery.GetDamageDecks damageDecks -> {
                    Game game = session.getGame();
                    DamageDecks decks = game.getDamageDecks();
                    yield Optional.of((T) decks);
                }
                case GameQuery.GetReadiness ready -> {
                    Map<Integer, Boolean> submitted = new HashMap<>();
                    for (Robot robot : session.getGame().getRobots()) {
                        PlayerID pid = new PlayerID(robot.getId());
                        submitted.put(robot.getId(), session.hasSubmitted(pid));
                    }
                    long msRemaining = session.getMillisecondsRemaining();
                    ReadinessDto dto = new ReadinessDto(submitted, msRemaining);
                    yield Optional.of((T) dto);
                }
                case GameQuery.GetTimeRemaining time -> {
                    Long ms = session.getMillisecondsRemaining();
                    yield Optional.of((T) ms);
                }

            };
        }
    }

    /**
     * Registers a manager-level observer.
     *
     * @param observer observer to add
     * @author Weihao Mo
     */
    public void addObserver(GameManagerObserver observer) {
        observers.add(observer);
    }

    /**
     * Unregisters a manager-level observer.
     *
     * @param observer observer to remove
     * @author Weihao Mo
     */
    public void removeObserver(GameManagerObserver observer) {
        observers.remove(observer);
    }

    /**
     * Broadcasts the start of the programming phase to observers.
     *
     * @param session the session in which programming started
     * @author William Pii Jæger
     */
    void broadcastProgrammingStarted(GameSession session) {
        for (GameManagerObserver obs : observers) {
            obs.onProgrammingStarted(session.getGame(), session.getGameId());
        }
    }

    /**
     * Broadcasts that a player has submitted their program.
     *
     * @param session  the session
     * @param playerId the submitting player
     * @author William Pii Jæger
     */
    void broadcastPlayerSubmitted(GameSession session, PlayerID playerId) {
        for (GameManagerObserver obs : observers) {
            obs.onPlayerSubmitted(session.getGame(), session.getGameId(), playerId);
        }
    }

    /**
     * Broadcasts that the round is executing.
     *
     * @param session the session
     * @author William Pii Jæger
     */
    void broadcastRoundExecuting(GameSession session) {
        for (GameManagerObserver obs : observers) {
            obs.onRoundExecuting(session.getGame(), session.getGameId());
        }
    }

    /**
     * Broadcasts that the game has finished.
     *
     * @param session the session
     * @author William Pii Jæger
     */
    void broadcastGameFinished(GameSession session) {
        for (GameManagerObserver obs : observers) {
            obs.onGameFinished(session.getGame(), session.getGameId());
        }
    }

    /**
     * Broadcasts that the robot needs respawn
     *
     * @param session the session
     * @param robotId id for the robot
     * @author Weihao Mo
     */
    void broadcastRobotNeedsRespawn(GameSession session, int robotId) {
        for (GameManagerObserver obs : observers) {
            obs.onRobotNeedsRespawn(session.getGame(), session.getGameId(), robotId);
        }
    }

    /**
     * Broadcasts that a reaction is needed from a player.
     *
     * @param session the session
     * @param request the reaction request
     *
     * @author William Pii Jæger
     */
    void broadcastReactionNeeded(GameSession session, ReactionRequest<?> request) {
        for (GameManagerObserver obs : observers) {
            obs.onReactionNeeded(session.getGame(), session.getGameId(), request);
        }
    }

    /**
     * Internal bridge from {@link RoundPacer.RoundPacerListener} to {@link GameManagerObserver} callbacks.
     *
     * @author William Pii Jæger
     */
    private class PacerBridge implements RoundPacer.RoundPacerListener {
        /**
         * Forwards programming-started event.
         *
         * @param session current session
         * @author William Pii Jæger
         */
        @Override
        public void onProgrammingStarted(GameSession session) {
            broadcastProgrammingStarted(session);
        }

        /**
         * Forwards player-submitted event.
         *
         * @param session  current session
         * @param playerId submitting player
         * @author William Pii Jæger
         */
        @Override
        public void onPlayerSubmitted(GameSession session, PlayerID playerId) {
            broadcastPlayerSubmitted(session, playerId);
        }

        /**
         * Forwards round-executing event.
         *
         * @param session current session
         * @author William Pii Jæger
         */
        @Override
        public void onRoundExecuting(GameSession session) {
            broadcastRoundExecuting(session);
        }

        /**
         * Forwards game-finished event.
         *
         * @param session current session
         * @author William Pii Jæger
         */
        @Override
        public void onGameFinished(GameSession session) {
            broadcastGameFinished(session);
        }

        /**
         * Forwards robot-respawn event.
         *
         * @param session current session
         * @param robotId id for the robot
         * @author Weihao Mo
         */
        @Override
        public void onRobotNeedsRespawn(GameSession session, int robotId) {
            broadcastRobotNeedsRespawn(session, robotId);
        }

        /**
         * Forwards reaction-needed event.
         *
         * @param session current session
         * @param request the reaction request
         *
         * @author William Pii Jæger
         */
        @Override
        public void onReactionNeeded(GameSession session, ReactionRequest<?> request) {
            broadcastReactionNeeded(session, request);
        }
    }

    /**
     * Notified when a winner is declared. Broadcast to manager observers and finishes the session
     *
     * @param game the game instance where the winner was declared
     * @param winner winning player ID
     * @author Weihao Mo
     */
    @Override
    public void onWinnerDeclared(Game game, PlayerID winner) {
        UUID gameId = null;
        GameSession session = null;

        for (Map.Entry<UUID, GameSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().getGame() == game) {
                gameId = entry.getKey();
                session = entry.getValue();
                break;
            }
        }

        if (gameId != null && session != null) {
            for (GameManagerObserver obs : observers) {
                obs.onWinnerDeclared(game, gameId, winner);
            }

            if (session.getState() != GameState.FINISHED) {
                session.cancelAutoExecuteTask();
                session.cancelStepTask();
                session.setState(GameState.FINISHED);
                broadcastGameFinished(session);
            }
        }
    }

    /**
     * Fans out a game update to observers associated with the corresponding session.
     *
     * @param game the game instance that triggered the update
     * @author William Pii Jæger
     * @author Niklas Emil Lysdal
     * @author Bjarke Søderhamn Petersen
     */
    @Override
    public void onGameUpdate(Game game) {
        UUID gameId = null;
        for (Map.Entry<UUID, GameSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().getGame() == game) {
                gameId = entry.getKey();
                break;
            }
        }
        if (gameId != null) {
            for (GameManagerObserver obs : observers) {
                obs.handleGameUpdate(game, gameId);
            }
        }
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public Map<UUID, GameSession> getActiveSessions() {
        return activeSessions;
    }
}