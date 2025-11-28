package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.ReactionRequest;

import java.util.UUID;

/**
 * @author William Pii Jæger
 */
public interface GameManagerObserver {
    default void handleGameUpdate(Game game, UUID gameID) {
    }

    default void onProgrammingStarted(Game game, UUID gameID) {
    }

    default void onPlayerSubmitted(Game game, UUID gameID, PlayerID playerId) {
    }

    default void onRoundExecuting(Game game, UUID gameID) {
    }

    default void onGameFinished(Game game, UUID gameID) {
    }

    default void onWinnerDeclared(Game game, UUID gameID, PlayerID winner) {
    }

    default void onRobotNeedsRespawn(Game game, UUID gameID, int robotId) {
    }

    default void onReactionNeeded(Game game, UUID gameID, ReactionRequest<?> request) {
    };
}