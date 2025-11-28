package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.*;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles timing, scheduling, and automatic execution flow for a single RoboRally game session.
 * Controls transitions between programming, executing, and finishing states.
 *
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public class GameScheduler implements RoundPacer {
    private static final long REGISTER_DELAY_MS = 800;
    private static final long PRE_ROUND_DELAY_MS = 300;
    private static final long NEXT_WINDOW_MS = 60_000L;
    private static final long EFFECT_DELAY_MS = 500;
    private static final long ROBOT_TURN_DELAY_MS = 400;
    private static final long RESPAWN_TIMEOUT_MS = 10_000L;
    private static final long REACTION_TIMEOUT_MS = 20_000L;

    private final ScheduledExecutorService scheduler;
    private final List<RoundPacerListener> listeners = new CopyOnWriteArrayList<>();

    private int currentRegister = 0;

    /**
     * Creates a default scheduler using a thread pool of size 4.
     *
     * @author William Pii Jæger
     */
    public GameScheduler() {
        this.scheduler = Executors.newScheduledThreadPool(4);
    }

    /**
     * Creates a scheduler using a provided executor service.
     *
     * @param scheduler custom scheduled executor service
     * @author William Pii Jæger
     */
    public GameScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Adds a listener to be notified about round pacing events.
     *
     * @param l the listener to add
     * @author William Pii Jæger
     */
    @Override
    public void addListener(RoundPacerListener l) {
        listeners.add(l);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param l the listener to remove
     * @author William Pii Jæger
     */
    @Override
    public void removeListener(RoundPacerListener l) {
        listeners.remove(l);
    }

    /**
     * Begins the programming phase for a session, setting deadlines,
     * clearing previous submissions, notifying listeners, and scheduling auto-execution.
     *
     * @param session  the current game session
     * @param windowMs time in milliseconds for players to submit programs
     * @author William Pii Jæger
     */
    @Override
    public void scheduleProgrammingPhase(GameSession session, long windowMs) {
        Instant deadline = Instant.now().plusMillis(windowMs);
        session.setProgrammingDeadline(deadline);
        session.setState(GameState.PROGRAMMING);
        session.clearSubmissions();

        currentRegister = 0;

        listeners.forEach(l -> l.onProgrammingStarted(session));

        ScheduledFuture<?> task = scheduler.schedule(
                () -> autoExecuteRound(session),
                windowMs,
                TimeUnit.MILLISECONDS
        );
        session.setAutoExecuteTask(task);
    }

    /**
     * Handles a player's program submission. If all players submit, cancels auto-execution
     * and immediately starts round execution.
     *
     * @param session  the current game session
     * @param playerId the player who submitted their program
     * @author William Pii Jæger
     */
    @Override
    public void onPlayerSubmitted(GameSession session, PlayerID playerId) {
        session.markSubmitted(playerId);
        listeners.forEach(l -> l.onPlayerSubmitted(session, playerId));

        if (session.allPlayersSubmitted()) {
            session.cancelAutoExecuteTask();
            executeRound(session);
        }
    }

    /**
     * Automatically executes the round if the programming phase expired without all submissions.
     * Any robot without a submission gets a random or default program.
     *
     * @param session the current game session
     * @author William Pii Jæger
     */
    private void autoExecuteRound(GameSession session) {
        if (session.getState() != GameState.PROGRAMMING) return;

        Game game = session.getGame();
        for (Robot robot : game.getRobots()) {
            PlayerID playerId = new PlayerID(robot.getId());
            if (!session.hasSubmitted(playerId)) {
                List<ProgramCard> randomCards = game.getRobotHand(robot.getId());
                if (randomCards.size() >= 5) robot.loadProgram(randomCards.subList(0, 5));
                else robot.loadProgram(randomCards);
            }
        }
        executeRound(session);
    }

    /**
     * Initiates execution of a round by setting the session state,
     * notifying listeners, and scheduling the first register execution.
     *
     * @param session the current game session
     * @author William Pii Jæger
     */
    private void executeRound(GameSession session) {
        session.setState(GameState.EXECUTING);
        listeners.forEach(l -> l.onRoundExecuting(session));

        Game game = session.getGame();
        game.applyTileEffects(Phase.ACTIVATE_ANTENNA);

        ScheduledFuture<?> pre = scheduler.schedule(
                () -> runRegister(session, 1),
                PRE_ROUND_DELAY_MS,
                TimeUnit.MILLISECONDS
        );
        session.setStepTask(pre);
    }

    /**
     * Executes a specific register sequentially with delays between robot turns,
     * handles winner detection, reboots, and transitions back to programming phase after all registers.
     *
     * @param session the current game session
     * @param reg     the register index (1-5)
     * @author William Pii Jæger
     */
    private void runRegister(GameSession session, int reg) {
        currentRegister = reg;

        ScheduledFuture<?> task = scheduler.schedule(() -> {
            try {
                Game game = session.getGame();

                for (Robot r : game.getRobots()) {
                    r.setMovedOnActivation(false);
                }

                List<Robot> robotsInOrder = game.getRobotsByPriority();

                executeRobotsSequentially(session, reg, robotsInOrder, 0);

            } catch (Exception e) {
                System.err.println("Error starting register " + reg + ": " + e.getMessage());
                e.printStackTrace();
                session.cancelStepTask();
                scheduleProgrammingPhase(session, NEXT_WINDOW_MS);
            }
        }, REGISTER_DELAY_MS, TimeUnit.MILLISECONDS);

        session.setStepTask(task);
    }

    /**
     * Recursively executes robot turns with delays between each robot.
     * Now checks for interactive cards and pauses for player input when needed.
     *
     * @param session the current game session
     * @param reg the register index (1-5)
     * @param robots list of robots in priority order
     * @param robotIndex current robot index being executed
     * @author William Pii Jæger
     */
    private void executeRobotsSequentially(GameSession session, int reg, List<Robot> robots, int robotIndex) {
        if (robotIndex >= robots.size()) {
            applyEffectsAndContinue(session, reg);
            return;
        }

        Game game = session.getGame();
        Robot currentRobot = robots.get(robotIndex);

        ProgramOP nextOp = peekNextOp(currentRobot);
        if (nextOp instanceof ProgramOP.Reaction reaction) {
            handleReaction(session, reg, robots, robotIndex, currentRobot, reaction);
            return;
        }

        game.executeOneRobotTurn(currentRobot);
        game.applyTileEffects(Phase.ACTIVATE_PITS);

        ScheduledFuture<?> nextTask = scheduler.schedule(
                () -> executeRobotsSequentially(session, reg, robots, robotIndex + 1),
                ROBOT_TURN_DELAY_MS,
                TimeUnit.MILLISECONDS
        );

        session.setStepTask(nextTask);
    }

    /**
     * Handles an interactive card by creating a reaction request and waiting for player input.
     * Stores the execution context so we can resume exactly where we paused.
     *
     * @author William Pii Jæger
     */
    private void handleReaction(GameSession session, int reg, List<Robot> robots,
                                int robotIndex, Robot robot, ProgramOP.Reaction reaction) {

        GameSession.ReactionExecutionContext context =
                new GameSession.ReactionExecutionContext(reg, robots, robotIndex);
        session.setReactionContext(context);

        ReactionSpec<?> spec = createReactionSpec(reaction.kind());
        ReactionId reactionId = ReactionId.random();
        Instant deadline = Instant.now().plusMillis(REACTION_TIMEOUT_MS);

        ReactionRequest<?> request = new ReactionRequest<>(
                reactionId,
                String.valueOf(robot.getId()),
                reg,
                0,
                spec,
                deadline
        );

        session.setPendingReaction(request);

        listeners.forEach(l -> l.onReactionNeeded(session, request));

        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            synchronized (session) {
                if (!session.isReactionResolved()) {
                    applyReactionChoice(session, robot, spec.defaultChoice());

                    continueAfterReaction(session);
                }
            }
        }, REACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        session.setReactionTimeoutTask(timeoutTask);
    }

    /**
     * Called when a player submits their reaction choice.
     * Applies the choice and continues execution from the stored context.
     *
     * @author William Pii Jæger
     */
    public void onReactionSubmitted(GameSession session, ReactionResolution<?> resolution) {
        synchronized (session) {
            ReactionRequest<?> pending = session.getPendingReaction();
            if (pending == null || !pending.id().equals(resolution.id())) {
                return;
            }

            session.setReactionResolution(resolution);
            session.cancelReactionTimeoutTask();

            Game game = session.getGame();
            Robot robot = game.getRobots().stream()
                    .filter(r -> String.valueOf(r.getId()).equals(pending.robotid()))
                    .findFirst()
                    .orElse(null);

            if (robot != null) {
                applyReactionChoice(session, robot, resolution.choice());
            }

            scheduler.schedule(() -> {
                continueAfterReaction(session);
            }, ROBOT_TURN_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Continues execution after a reaction has been resolved.
     * Uses the stored execution context to resume at the exact point where we paused.
     *
     * @author William Pii Jæger
     */
    private void continueAfterReaction(GameSession session) {
        GameSession.ReactionExecutionContext context = session.getReactionContext();
        if (context == null) {
            System.err.println("No reaction context found - cannot continue");
            return;
        }

        Game game = session.getGame();
        game.applyTileEffects(Phase.ACTIVATE_PITS);

        session.clearReaction();

        executeRobotsSequentially(
                session,
                context.getRegisterIndex(),
                context.getRobotsInOrder(),
                context.getRobotIndex() + 1
        );
    }

    /**
     * Applies the chosen reaction to the robot.
     * Replaces the Reaction operation with the chosen operation.
     *
     * @author William Pii Jæger
     */
    private void applyReactionChoice(GameSession session, Robot robot, ReactionChoice choice) {
        Game game = session.getGame();

        robot.pollNextOp();

        switch (choice) {
            case ReactionChoice.SandBoxChoice sandbox -> {
                switch (sandbox) {
                    case MOVE1 -> robot.getRegisters().addFirst(new ProgramOP.Move(1));
                    case MOVE2 -> robot.getRegisters().addFirst(new ProgramOP.Move(2));
                    case MOVE3 -> robot.getRegisters().addFirst(new ProgramOP.Move(3));
                    case BACKUP -> robot.getRegisters().addFirst(new ProgramOP.Move(-1));
                    case LEFT -> robot.getRegisters().addFirst(new ProgramOP.RotateLeft());
                    case RIGHT -> robot.getRegisters().addFirst(new ProgramOP.RotateRight());
                    case UTURN -> robot.getRegisters().addFirst(new ProgramOP.UTurn());
                }
            }
            case ReactionChoice.WeaselChoice weasel -> {
                switch (weasel) {
                    case LEFT -> robot.getRegisters().addFirst(new ProgramOP.RotateLeft());
                    case RIGHT -> robot.getRegisters().addFirst(new ProgramOP.RotateRight());
                    case UTURN -> robot.getRegisters().addFirst(new ProgramOP.UTurn());
                }
            }
            case ReactionChoice.SpeedChoice speed -> {
                if (speed == ReactionChoice.SpeedChoice.MOVE3) {
                    robot.getRegisters().addFirst(new ProgramOP.Move(3));
                }
            }
        }

        game.executeOneRobotTurn(robot);
    }

    /**
     * Creates a reaction spec for a given reaction kind.
     *
     * @author William Pii Jæger
     */
    @SuppressWarnings("unchecked")
    private <C extends ReactionChoice> ReactionSpec<C> createReactionSpec(ReactionKind kind) {
        return switch (kind) {
            case SANDBOX -> (ReactionSpec<C>) new ReactionSpec<>(
                    ReactionKind.SANDBOX,
                    List.of(ReactionChoice.SandBoxChoice.values()),
                    ReactionChoice.SandBoxChoice.MOVE1
            );
            case WEASEL -> (ReactionSpec<C>) new ReactionSpec<>(
                    ReactionKind.WEASEL,
                    List.of(ReactionChoice.WeaselChoice.values()),
                    ReactionChoice.WeaselChoice.LEFT
            );
            case SPEED -> (ReactionSpec<C>) new ReactionSpec<>(
                    ReactionKind.SPEED,
                    List.of(ReactionChoice.SpeedChoice.values()),
                    ReactionChoice.SpeedChoice.MOVE3
            );
        };
    }

    /**
     * Peeks at the next operation without removing it from the queue.
     *
     * @author William Pii Jæger
     */
    private ProgramOP peekNextOp(Robot robot) {
        return robot.getRegisters().peekFirst();
    }

    /**
     * Applies board effects after all robots have moved in a register,
     * checks for dead robots after EACH register, then continues to next register or programming phase.
     *
     * @param session the current game session
     * @param reg the register index that just completed
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    private void applyEffectsAndContinue(GameSession session, int reg) {
        ScheduledFuture<?> effectsTask = scheduler.schedule(() -> {
            try {
                Game game = session.getGame();
                game.applyBoardEffectsAfterRegister();

                if (game.getWinner().isPresent()) {
                    session.setState(GameState.FINISHED);
                    listeners.forEach(l -> l.onGameFinished(session));
                    session.cancelStepTask();
                    return;
                }

                List<Robot> deadRobots = game.getDeadRobots();
                if (!deadRobots.isEmpty()) {
                    for (Robot robot : deadRobots) {
                        listeners.forEach(l -> l.onRobotNeedsRespawn(session, robot.getId()));
                    }
                    session.setDeadRobotsAwaitingRespawn(deadRobots.size());

                    scheduleRespawnTimeout(session, reg);
                    return;
                }

                if (reg < 5) {
                    runRegister(session, reg + 1);
                } else {
                    game.dealNewHands();
                    session.cancelStepTask();
                    scheduleProgrammingPhase(session, NEXT_WINDOW_MS);
                }
            } catch (Exception e) {
                System.err.println("Error in effects of register " + reg + ": " + e.getMessage());
                e.printStackTrace();
                session.cancelStepTask();
                scheduleProgrammingPhase(session, NEXT_WINDOW_MS);
            }
        }, EFFECT_DELAY_MS, TimeUnit.MILLISECONDS);

        session.setStepTask(effectsTask);
    }

    /**
     * Schedules timeout for the direction selecting
     * @author Weihao Mo
     */
    private void scheduleRespawnTimeout(GameSession session, int reg) {
        ScheduledFuture<?> respawnTimeout = scheduler.schedule(() -> {
            if (!session.allRespawnDirectionsSet()) {
                Game game = session.getGame();
                Random random = new Random();
                Direction[] directions = Direction.values();

                for (Robot robot : game.getDeadRobots()) {
                    if (robot.getRespawnDirection() == null) {
                        Direction randomDirection = directions[random.nextInt(directions.length)];
                        robot.setRespawnDirection(randomDirection);
                        session.markRespawnDirectionSet(robot.getId());
                    }
                }
            }

            continueAfterRespawn(session, reg);
        }, RESPAWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        session.setRespawnTimeoutTask(respawnTimeout);
    }

    /**
     * Continues the game after respawn directions have been set (either manually or automatically).
     * This respawns robots immediately and continues with the next register.
     *
     * @param session the current game session
     * @param reg the current register index
     * @author Weihao Mo
     */
    private void continueAfterRespawn(GameSession session, int reg) {
        Game game = session.getGame();

        game.applyTileEffects(Phase.ACTIVATE_REBOOT);
        game.rebootRobots();
        session.clearDeadRobotsAwaitingRespawn();

        if (reg < 5) {
            runRegister(session, reg + 1);
        } else {
            game.dealNewHands();
            session.cancelStepTask();
            scheduleProgrammingPhase(session, NEXT_WINDOW_MS);
        }
    }

    /**
     * Called when all robots have set their respawn directions
     * Cancels the timeout and continues
     *
     * @param session the current game session
     * @author Weihao Mo
     */
    public void continueAfterAllRespawns(GameSession session) {
        session.cancelRespawnTimeoutTask();
        continueAfterRespawn(session, currentRegister);
    }

    /**
     * Shuts down the scheduler gracefully, waiting up to 5 seconds before forcing shutdown.
     *
     * @author William Pii Jæger
     */
    @Override
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}