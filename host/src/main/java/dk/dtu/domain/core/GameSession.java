package dk.dtu.domain.core;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

/**
 * Per-game session state and synchronization gates for scheduler-driven flow.
 * <p>
 * Thread-safety: methods are synchronized where needed to coordinate with
 * {@link java.util.concurrent.ScheduledExecutorService} callbacks in {@link GameScheduler}.
 * </p>
 *
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public class GameSession {
    private final UUID gameId;
    private final Game game;
    private GameState state;
    private Instant programmingDeadline;
    private final Set<PlayerID> submittedPlayers;
    private ScheduledFuture<?> autoExecuteTask;
    private ScheduledFuture<?> stepTask;
    private ScheduledFuture<?> respawnTimeoutTask;
    private final int totalPlayers;
    private int deadRobotsAwaitingRespawn = 0;
    private final Set<Integer> robotsWithRespawnDirection = new HashSet<>();

    /**
     * Creates a new session around a specific game.
     *
     * @param gameId the unique session ID
     * @param game   the game instance
     * @author William Pii Jæger
     */
    public GameSession(UUID gameId, Game game) {
        this.gameId = gameId;
        this.game = game;
        this.state = GameState.WAITING_TO_START;
        this.submittedPlayers = new HashSet<>();
        this.totalPlayers = game.getRobots().size();
    }

    /**
     * @return session ID
     * @author William Pii Jæger
     */
    public UUID getGameId() {
        return gameId;
    }

    /**
     * @return the game bound to this session
     * @author William Pii Jæger
     */
    public Game getGame() {
        return game;
    }

    /**
     * @return current session state
     * @author William Pii Jæger
     */
    public synchronized GameState getState() {
        return state;
    }

    /**
     * Sets the session state.
     *
     * @param state new state
     * @author William Pii Jæger
     */
    public synchronized void setState(GameState state) {
        this.state = state;
    }

    /**
     * Sets the deadline for the programming phase.
     *
     * @param deadline deadline instant
     * @author William Pii Jæger
     */
    public synchronized void setProgrammingDeadline(Instant deadline) {
        this.programmingDeadline = deadline;
    }

    /**
     * Checks if a player has submitted their program.
     *
     * @param playerId player to check
     * @return true if submitted; false otherwise
     * @author William Pii Jæger
     */
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

    public synchronized void setRespawnTimeoutTask(ScheduledFuture<?> task) {
        this.respawnTimeoutTask = task;
    }

    public synchronized void cancelRespawnTimeoutTask() {
        if (respawnTimeoutTask != null && !respawnTimeoutTask.isDone()) {
            respawnTimeoutTask.cancel(false);
        }
        respawnTimeoutTask = null;
    }

    public synchronized long getMillisecondsRemaining() {
        if (programmingDeadline == null || state != GameState.PROGRAMMING) {
            return 0;
        }
        long ms = programmingDeadline.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0, ms);
    }

    /**
     * Set a timeout for respawn direction setting
     *
     * @param count for timeout
     * @author Weihao Mo
     */
    public synchronized void setDeadRobotsAwaitingRespawn(int count) {
        this.deadRobotsAwaitingRespawn = count;
        this.robotsWithRespawnDirection.clear();
    }

    /**
     * Mark the robot that has set the respawn direction
     *
     * @param robotId id of the robot
     * @author Weihao Mo
     */
    public synchronized void markRespawnDirectionSet(int robotId) {
        robotsWithRespawnDirection.add(robotId);
    }


    /**
     * Checks if all dead robots have set their direction for respawn
     *
     * @return true if submitted direction; false otherwise
     * @author Weihao Mo
     */
    public synchronized boolean allRespawnDirectionsSet() {
        return deadRobotsAwaitingRespawn > 0 &&
                robotsWithRespawnDirection.size() >= deadRobotsAwaitingRespawn;
    }

    /**
     * Clean up all setup and respawn directions for future registration
     *
     * @author Weihao Mo
     */
    public synchronized void clearDeadRobotsAwaitingRespawn() {
        this.deadRobotsAwaitingRespawn = 0;
        this.robotsWithRespawnDirection.clear();
    }
}