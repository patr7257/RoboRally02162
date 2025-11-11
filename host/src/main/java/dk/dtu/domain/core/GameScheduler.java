package dk.dtu.domain.core;

import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

// Author(s) William Pii Jæger

public class GameScheduler implements RoundPacer {
    private static final long REGISTER_DELAY_MS = 800;
    private static final long PRE_ROUND_DELAY_MS = 300;
    private static final long NEXT_WINDOW_MS = 60_000L;

    private final ScheduledExecutorService scheduler;
    private final List<RoundPacerListener> listeners = new CopyOnWriteArrayList<>();

    public GameScheduler() {
        this.scheduler = Executors.newScheduledThreadPool(4);
    }

    public GameScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void addListener(RoundPacerListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(RoundPacerListener l) {
        listeners.remove(l);
    }

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

    @Override
    public void onPlayerSubmitted(GameSession session, PlayerID playerId) {
        session.markSubmitted(playerId);
        listeners.forEach(l -> l.onPlayerSubmitted(session, playerId));

        if (session.allPlayersSubmitted()) {
            session.cancelAutoExecuteTask();
            executeRound(session);
        }
    }

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

    private void runRegister(GameSession session, int reg) {
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            try {
                Game game = session.getGame();

                game.executeRegister(reg);

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
                System.err.println("Error in register " + reg + ": " + e.getMessage());
                e.printStackTrace();
                session.cancelStepTask();
                scheduleProgrammingPhase(session, NEXT_WINDOW_MS);
            }
        }, REGISTER_DELAY_MS, TimeUnit.MILLISECONDS);

        session.setStepTask(task);
    }

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
