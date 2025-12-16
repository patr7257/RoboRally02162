package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.ReactionRequest;

import java.util.UUID;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public interface GameManagerObserver {
    default void handleGameUpdate(Game game, UUID gameID) {
    }

    default void onProgrammingStarted(Game game, UUID gameID) {
    }

    default void onPlayerSubmitted(Game game, UUID gameID, int robotId) {
    }

    default void onRoundExecuting(Game game, UUID gameID) {
    }

    default void onGameFinished(Game game, UUID gameID) {
    }

    default void onWinnerDeclared(Game game, UUID gameID, int winner) {
    }

    default void onRobotNeedsRespawn(Game game, UUID gameID, int robotId) {
    }

    default void onReactionNeeded(Game game, UUID gameID, ReactionRequest<?> request) {
    }
    default void onTileEffectActivated(Game game, UUID gameID, int x, int y, String effectKind) { }
}