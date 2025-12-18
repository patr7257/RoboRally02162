package dk.dtu.support;

import dk.dtu.domain.core.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// This is kinda bad
// We have repeat logic, but this one just does not delay
// This mean if we update the actual GameScheduler, we need to make sure
// those changes are reflected here.

/**
 * @author William Pii Jæger
 */
public class NoDelayPacer implements RoundPacer {
    private final List<RoundPacerListener> listeners = new CopyOnWriteArrayList<>();

    @Override public void addListener(RoundPacerListener l) { listeners.add(l); }
    @Override public void removeListener(RoundPacerListener l) { listeners.remove(l); }

    @Override
    public void onRobotRespawnDirectionSet(GameSession session, int reg) {}

    @Override public void shutdown() {}

    @Override
    public void scheduleProgrammingPhase(GameSession s, long windowMs) {
        synchronized (s) {
            s.setProgrammingDeadline(Instant.now().plusMillis(windowMs));
            s.setState(GameState.PROGRAMMING);
            s.clearSubmissions();
        }
        listeners.forEach(l -> l.onProgrammingStarted(s));
    }

    @Override
    public void onPlayerSubmitted(GameSession s, int rid) {
        synchronized (s) {
            s.markSubmitted(rid);
        }
        listeners.forEach(l -> l.onPlayerSubmitted(s, rid));
    }

    public void runAllRegisters(GameSession s) {
        listeners.forEach(l -> l.onRoundExecuting(s));
        boolean finished;
        synchronized (s) {
            s.setState(GameState.EXECUTING);
            Game g = s.getGame();
            finished = false;
            for (int reg = 1; reg <= 5; reg++) {
                g.executeRegister(reg);
                if (g.getWinner().isPresent()) {
                    s.setState(GameState.FINISHED);
                    finished = true;
                    break;
                }
            }
            if (!finished) g.dealNewHands();
        }
        if (finished) listeners.forEach(l -> l.onGameFinished(s));
    }
}
