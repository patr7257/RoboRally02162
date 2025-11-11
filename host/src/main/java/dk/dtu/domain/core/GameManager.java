package dk.dtu.domain.core;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Deck;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.ReadinessDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;

import java.util.*;

public class GameManager implements GameObserver {
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final List<GameManagerObserver> observers = new ArrayList<>();
    private final RoundPacer pacer;

    // Author(s) William Pii Jæger
    public GameManager(RoundPacer pacer) {
        this.pacer = pacer;
        this.pacer.addListener(new PacerBridge());
    }

    // Author(s) William Pii Jæger, Weihao Mo
    public UUID startGame(Board board, BoardAPI api, List<Robot> players) {
        UUID id = UUID.randomUUID();
        Game game = new Game(board, api, players);
        game.addObserver(this);
        GameSession session = new GameSession(id, game);
        activeSessions.put(id, session);
        return id;
    }


    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    public UUID startGame(Board board, BoardAPI api, List<Robot> players, Map<Integer, Deck> decks) {
        UUID id = UUID.randomUUID();
        Game game = new Game(board, api, players, decks);
        game.addObserver(this);
        GameSession session = new GameSession(id, game);
        activeSessions.put(id, session);
        return id;
    }


    // Author(s) William Pii Jæger, Weihao Mo
    public void endGame(UUID id) {
        GameSession session = activeSessions.remove(id);
        if (session != null) {
            session.cancelAutoExecuteTask();
            session.cancelStepTask();
            session.setState(GameState.FINISHED);
        }
    }

    // Author(s) William Pii Jæger
    public Optional<GameSession> findSessionByID(UUID gameID) {
        return Optional.ofNullable(activeSessions.get(gameID));
    }

    // Author(s) William Pii Jæger
    public Optional<Game> findByID(UUID gameID) {
        return findSessionByID(gameID).map(GameSession::getGame);
    }

    // Author(s) William Pii Jæger
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
        };
    }

    // Author(s) William Pii Jæger
    public synchronized <T> Optional<T> query(UUID gameId, GameQuery<T> query) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) return Optional.empty();

        // These are unsafe generic casts
        // We know the returned types are correct, but java does not
        // We could have individual getters for each, but I think this is fine
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

    // Author(s): Weihao Mo
    public void addObserver(GameManagerObserver observer) {
        observers.add(observer);
    }

    // Author(s): Weihao Mo
    public void removeObserver(GameManagerObserver observer) {
        observers.remove(observer);
    }

    // Author(s): William Pii Jæger
    void broadcastProgrammingStarted(GameSession session) {
        for (GameManagerObserver obs : observers) {
            obs.onProgrammingStarted(session.getGame(), session.getGameId());
        }
    }

    // Author(s): William Pii Jæger
    void broadcastPlayerSubmitted(GameSession session, PlayerID playerId) {
        for (GameManagerObserver obs : observers) {
            obs.onPlayerSubmitted(session.getGame(), session.getGameId(), playerId);
        }
    }

    // Author(s): William Pii Jæger
    void broadcastRoundExecuting(GameSession session) {
        for (GameManagerObserver obs : observers) {
            obs.onRoundExecuting(session.getGame(), session.getGameId());
        }
    }

    // Author(s): William Pii Jæger
    void broadcastGameFinished(GameSession session) {
        for (GameManagerObserver obs : observers) {
            obs.onGameFinished(session.getGame(), session.getGameId());
        }
    }

    // Author(s): William Pii Jæger
    private class PacerBridge implements RoundPacer.RoundPacerListener {
        @Override
        public void onProgrammingStarted(GameSession session) {
            broadcastProgrammingStarted(session);
        }

        @Override
        public void onPlayerSubmitted(GameSession session, PlayerID playerId) {
            broadcastPlayerSubmitted(session, playerId);
        }

        @Override
        public void onRoundExecuting(GameSession session) {
            broadcastRoundExecuting(session);
        }

        @Override
        public void onGameFinished(GameSession session) {
            broadcastGameFinished(session);
        }
    }

    // Author(s): Weihao Mo
    @Override
    public void onWinnerDeclared(PlayerID winner) {
    }

    // Author(s): William Pii Jæger, Niklas, Bjarke
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
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    public Map<UUID, GameSession> getActiveSessions() {
        return activeSessions;
    }
}
