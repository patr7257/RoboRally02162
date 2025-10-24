package dk.dtu.domain.core;

// Author(s) William Pii Jæger

// Roundabout attempt at avoiding a shitty circular dependency, written at 00:25 in the morning!

public interface RoundPacer {
    void scheduleProgrammingPhase(GameSession session, long windowMs);

    void onPlayerSubmitted(GameSession session, PlayerID playerId);

    void shutdown();

    void addListener(RoundPacerListener l);

    void removeListener(RoundPacerListener l);

    interface RoundPacerListener {
        void onProgrammingStarted(GameSession session);

        void onPlayerSubmitted(GameSession session, PlayerID playerId);

        void onRoundExecuting(GameSession session);

        void onGameFinished(GameSession session);
    }
}
