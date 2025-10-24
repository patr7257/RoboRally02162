package dk.dtu.domain.core;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

// Author(s) William Pii Jæger

// Because GameScheduler uses ScheduledExecutorService we use synchronized func here
// I honestly have no clue it if even affects the (possible) race conditions or not

public class GameSession {
    private final UUID gameId;
    private final Game game;
    private GameState state;
    private Instant programmingDeadline;
    private final Set<PlayerID> submittedPlayers;
    private ScheduledFuture<?> autoExecuteTask;
    private ScheduledFuture<?> stepTask;
    private final int totalPlayers;

    public GameSession(UUID gameId, Game game) {
        this.gameId = gameId;
        this.game = game;
        this.state = GameState.WAITING_TO_START;
        this.submittedPlayers = new HashSet<>();
        this.totalPlayers = game.getRobots().size();
    }

    public UUID getGameId() {
        return gameId;
    }

    public Game getGame() {
        return game;
    }

    public synchronized GameState getState() {
        return state;
    }

    public synchronized void setState(GameState state) {
        this.state = state;
    }

    public synchronized void setProgrammingDeadline(Instant deadline) {
        this.programmingDeadline = deadline;
    }

    public synchronized boolean hasSubmitted(PlayerID playerId) {
        return submittedPlayers.contains(playerId);
    }

    public synchronized void markSubmitted(PlayerID playerId) {
        submittedPlayers.add(playerId);
    }

    public synchronized boolean allPlayersSubmitted() {
        return submittedPlayers.size() == totalPlayers;
    }

    public synchronized Set<PlayerID> getSubmittedPlayers() {
        return new HashSet<>(submittedPlayers);
    }

    public synchronized void clearSubmissions() {
        submittedPlayers.clear();
    }

    public synchronized void setAutoExecuteTask(ScheduledFuture<?> task) {
        this.autoExecuteTask = task;
    }

    public synchronized void cancelAutoExecuteTask() {
        if (autoExecuteTask != null && !autoExecuteTask.isDone()) {
            autoExecuteTask.cancel(false);
        }
        autoExecuteTask = null;
    }

    public synchronized void setStepTask(ScheduledFuture<?> task) {
        this.stepTask = task;
    }

    public synchronized void cancelStepTask() {
        if (stepTask != null && !stepTask.isDone()) {
            stepTask.cancel(false);
        }
        stepTask = null;
    }

    public synchronized long getMillisecondsRemaining() {
        if (programmingDeadline == null || state != GameState.PROGRAMMING) {
            return 0;
        }
        long ms = programmingDeadline.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0, ms);
    }
}
