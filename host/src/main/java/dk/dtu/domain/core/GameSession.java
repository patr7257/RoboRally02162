package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.*;
import dk.dtu.domain.model.Robot;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
    private final Set<Integer> submittedPlayers;
    private ScheduledFuture<?> autoExecuteTask;
    private ScheduledFuture<?> stepTask;
    private ScheduledFuture<?> respawnTimeoutTask;
    private final int totalPlayers;

    private boolean demoMode = false;
    private DemoTimingConfig demoTimingConfig = null;

    private ReactionRequest<?> pendingReaction;
    private ReactionResolution<?> reactionResolution;
    private ScheduledFuture<?> reactionTimeoutTask;
    private ReactionExecutionContext reactionContext;

    private RespawnRequest pendingRespawn;
    private RespawnResolution respawnResolution;
    private ScheduledFuture<?> singleRespawnTimeoutTask;
    private RespawnExecutionContext respawnExecutionContext;

    /**
     * Stores execution context when pausing for a reaction.
     * Allows resuming at the exact point where we paused.
     *
     * @author William Pii Jæger
     */
    public static class ReactionExecutionContext {
        private final int registerIndex;
        private final List<Robot> robotsInOrder;
        private final int robotIndex;

        public ReactionExecutionContext(int registerIndex, List<Robot> robotsInOrder, int robotIndex) {
            this.registerIndex = registerIndex;
            this.robotsInOrder = robotsInOrder;
            this.robotIndex = robotIndex;
        }

        public int getRegisterIndex() { return registerIndex; }
        public List<Robot> getRobotsInOrder() { return robotsInOrder; }
        public int getRobotIndex() { return robotIndex; }
    }

    /**
     * Stores execution context when pausing for a respawn event.
     * Allows resuming at the exact point where we paused.
     *
     * @author Weihao Mo
     */
    public static class RespawnExecutionContext {
        private final int registerIndex;
        private final List<Robot> deadRobots;
        private final int robotIndex;

        public RespawnExecutionContext(int registerIndex, List<Robot> deadRobots, int robotIndex) {
            this.registerIndex = registerIndex;
            this.deadRobots = deadRobots;
            this.robotIndex = robotIndex;
        }

        public int getRegisterIndex() { return registerIndex; }
        public List<Robot> getDeadRobots() { return deadRobots; }
        public int getRobotIndex() { return robotIndex; }
    }

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
        this.demoTimingConfig = DemoTimingConfig.defaultConfig();
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
     * @param robotId player to check
     * @return true if submitted; false otherwise
     * @author William Pii Jæger
     */
    public synchronized boolean hasSubmitted(int robotId) {
        return submittedPlayers.contains(robotId);
    }

    public synchronized void markSubmitted(int robotId) {
        submittedPlayers.add(robotId);
    }

    public synchronized boolean allPlayersSubmitted() {
        return submittedPlayers.size() == totalPlayers;
    }

    public synchronized Set<Integer> getSubmittedPlayers() {
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

    /**
     * @author Weihao Mo
     */
    public synchronized void setRespawnTimeoutTask(ScheduledFuture<?> task) {
        this.respawnTimeoutTask = task;
    }


    /**
     * @author Weihao Mo
     */
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
     * @author Weihao Mo
     */
    public synchronized void setPendingRespawn(RespawnRequest request) {
        this.pendingRespawn = request;
        this.respawnResolution = null;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized RespawnRequest getPendingRespawn() {
        return pendingRespawn;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized void clearRespawn() {
        this.pendingRespawn = null;
        this.respawnResolution = null;
        this.respawnExecutionContext = null;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized void setRespawnResolution(RespawnResolution resolution) {
        this.respawnResolution = resolution;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized boolean isRespawnResolved() {
        return respawnResolution != null;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized RespawnResolution getRespawnResolution() {
        return respawnResolution;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized void setSingleRespawnTimeoutTask(ScheduledFuture<?> task) {
        this.singleRespawnTimeoutTask = task;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized void cancelSingleRespawnTimeoutTask() {
        if (singleRespawnTimeoutTask != null && !singleRespawnTimeoutTask.isDone()) {
            singleRespawnTimeoutTask.cancel(false);
        }
        singleRespawnTimeoutTask = null;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized void setRespawnExecutionContext(RespawnExecutionContext context) {
        this.respawnExecutionContext = context;
    }

    /**
     * @author Weihao Mo
     */
    public synchronized RespawnExecutionContext getRespawnExecutionContext() {
        return respawnExecutionContext;
    }

    /**
     * Sets a pending reaction request that needs to be resolved.
     *
     * @author William Pii Jæger
     */
    public synchronized void setPendingReaction(ReactionRequest<?> request) {
        this.pendingReaction = request;
        this.reactionResolution = null;
    }

    /**
     * Gets the current pending reaction request, if any.
     *
     * @author William Pii Jæger
     */
    public synchronized ReactionRequest<?> getPendingReaction() {
        return pendingReaction;
    }

    /**
     * Clears the pending reaction and its resolution.
     *
     * @author William Pii Jæger
     */
    public synchronized void clearReaction() {
        this.pendingReaction = null;
        this.reactionResolution = null;
        this.reactionContext = null;
    }

    /**
     * Sets the resolution for the current pending reaction.
     *
     * @author William Pii Jæger
     */
    public synchronized void setReactionResolution(ReactionResolution<?> resolution) {
        this.reactionResolution = resolution;
    }

    /**
     * Checks if the pending reaction has been resolved.
     *
     * @author William Pii Jæger
     */
    public synchronized boolean isReactionResolved() {
        return reactionResolution != null;
    }

    /**
     * Sets the timeout task for reaction resolution.
     *
     * @author William Pii Jæger
     */
    public synchronized void setReactionTimeoutTask(ScheduledFuture<?> task) {
        this.reactionTimeoutTask = task;
    }

    /**
     * Cancels the reaction timeout task.
     *
     * @author William Pii Jæger
     */
    public synchronized void cancelReactionTimeoutTask() {
        if (reactionTimeoutTask != null && !reactionTimeoutTask.isDone()) {
            reactionTimeoutTask.cancel(false);
        }
        reactionTimeoutTask = null;
    }

    /**
     * Stores the execution context when pausing for a reaction.
     *
     * @author William Pii Jæger
     */
    public synchronized void setReactionContext(ReactionExecutionContext context) {
        this.reactionContext = context;
    }

    /**
     * Gets the stored execution context.
     *
     * @author William Pii Jæger
     */
    public synchronized ReactionExecutionContext getReactionContext() {
        return reactionContext;
    }

    /**
     * Checks if this session is in demo mode.
     * Demo mode bypasses card validation and allows custom timing.
     *
     * @return true if demo mode is enabled
     * @author William Pii Jæger
     */
    public boolean isDemoMode() {
        return demoMode;
    }

    /**
     * Enables or disables demo mode for this session.
     * Demo mode should only be toggled when not actively executing.
     *
     * @param demoMode true to enable demo mode, false to disable
     * @author William Pii Jæger
     */
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    /**
     * Gets the custom timing configuration for demo mode.
     *
     * @return the demo timing config, or null if using defaults
     * @author William Pii Jæger
     */
    public DemoTimingConfig getDemoTimingConfig() {
        return demoTimingConfig;
    }

    /**
     * Sets custom timing configuration for demo mode.
     *
     * @param config the timing configuration to use
     * @author William Pii Jæger
     */
    public void setDemoTimingConfig(DemoTimingConfig config) {
        this.demoTimingConfig = config;
    }
}