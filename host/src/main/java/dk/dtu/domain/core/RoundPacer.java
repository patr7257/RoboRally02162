package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.ReactionRequest;

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
    void onPlayerSubmitted(GameSession session, int robotId);

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
    void onRobotRespawnDirectionSet(GameSession session, int reg);

    interface RoundPacerListener {
        /**
         * @author William Pii Jæger
         */
        void onProgrammingStarted(GameSession session);

        /**
         * @author William Pii Jæger
         */
        void onPlayerSubmitted(GameSession session, int robotId);

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

        /**
         * @author William Pii Jæger
         */
        void onReactionNeeded(GameSession session, ReactionRequest<?> request);
    }
}
