package dk.dtu.domain.core;

import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles timing, scheduling, and automatic execution flow for a single RoboRally game session.
 * Controls transitions between programming, executing, and finishing states.
 *
 * @author William Pii Jæger
 */
public class GameScheduler implements RoundPacer {
    private static final long REGISTER_DELAY_MS = 800;
    private static final long PRE_ROUND_DELAY_MS = 300;
    private static final long NEXT_WINDOW_MS = 60_000L;
    private static final long EFFECT_DELAY_MS = 500;

    private static final long ROBOT_TURN_DELAY_MS = 400;



    private final ScheduledExecutorService scheduler;
    private final List<RoundPacerListener> listeners = new CopyOnWriteArrayList<>();

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

        game.executeOneRobotTurn(currentRobot);

        ScheduledFuture<?> nextTask = scheduler.schedule(
                () -> executeRobotsSequentially(session, reg, robots, robotIndex + 1),
                ROBOT_TURN_DELAY_MS,
                TimeUnit.MILLISECONDS
        );

        session.setStepTask(nextTask);
    }

    /**
     * Applies board effects after all robots have moved in a register,
     * then continues to next register or programming phase.
     *
     * @param session the current game session
     * @param reg the register index that just completed
     * @author William Pii Jæger
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

                if (reg < 5) {
                    runRegister(session, reg + 1);
                } else {
                    game.dealNewHands();
                    game.rebootRobots();
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
