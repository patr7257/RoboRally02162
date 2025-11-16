package dk.dtu.domain.core;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public interface RoundPacer {
    /**
     * @author William Pii Jæger
     */
    void scheduleProgrammingPhase(GameSession session, long windowMs);

    /**
     * @author William Pii Jæger
     */
    void onPlayerSubmitted(GameSession session, PlayerID playerId);

    /**
     * @author William Pii Jæger
     */
    void shutdown();

    /**
     * @author William Pii Jæger
     */
    void addListener(RoundPacerListener l);

    /**
     * @author William Pii Jæger
     */
    void removeListener(RoundPacerListener l);

    /**
     * @author Weihao Mo
     */
    void continueAfterAllRespawns(GameSession session);

    interface RoundPacerListener {
        /**
         * @author William Pii Jæger
         */
        void onProgrammingStarted(GameSession session);

        /**
         * @author William Pii Jæger
         */
        void onPlayerSubmitted(GameSession session, PlayerID playerId);

        /**
         * @author William Pii Jæger
         */
        void onRoundExecuting(GameSession session);

        /**
         * @author William Pii Jæger
         */
        void onGameFinished(GameSession session);

        /**
         * @author Weihao Mo
         */
        void onRobotNeedsRespawn(GameSession session, int robotId);
    }
}
