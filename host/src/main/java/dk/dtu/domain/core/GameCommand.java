package dk.dtu.domain.core;

import dk.dtu.domain.core.reaction.ReactionChoice;
import dk.dtu.domain.core.reaction.ReactionId;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.program.ProgramCard;

import java.util.List;
import java.util.UUID;

/**
 * A set of game commands is provided through a sealed interface that controls the game flow
 * Available commands:
 * <ul>
 *   <li>{@link StartProgramming} - Initiates the programming phase with a time window</li>
 *   <li>{@link SubmitPrograms} - Submits a player's program card selection</li>
 *   <li>{@link EndGame} - Terminates the game session</li>
 *   <li>{@link SetRespawnDirection} - Sets the respawn direction for a dead robot</li>
 * </ul>
 * </p>
 *
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public sealed interface GameCommand permits GameCommand.EndGame, GameCommand.ForceStartRound, GameCommand.SetDemoTimings, GameCommand.SetRespawnDirection, GameCommand.StartProgramming, GameCommand.SubmitPrograms, GameCommand.SubmitReaction, GameCommand.ToggleDemo {

    record StartProgramming(UUID commandId, UUID gameId, long windowMs) implements GameCommand {
    }

    record SubmitPrograms(UUID commandId, UUID gameId, PlayerID player,
                          List<ProgramCard> cards) implements GameCommand {
    }

    record EndGame(UUID commandId, UUID gameId) implements GameCommand {
    }

    record SetRespawnDirection(UUID commandId, UUID gameId, PlayerID player, Direction direction) implements GameCommand{
    }

    record SubmitReaction(UUID commandId, UUID gameId, PlayerID player, ReactionId reactionId, ReactionChoice choice) implements GameCommand {}

    /**
     * Toggles demo mode for a game session.
     * Demo mode bypasses card validation and allows custom timing.
     *
     * @param commandId unique command identifier
     * @param gameId the game to toggle demo mode for
     * @author William Pii Jæger
     */
    record ToggleDemo(UUID commandId, UUID gameId) implements GameCommand {
    }

    /**
     * Sets custom timing configuration for demo mode.
     *
     * @param commandId unique command identifier
     * @param gameId the game to set timings for
     * @param timings the timing configuration to apply
     * @author William Pii Jæger
     */
    record SetDemoTimings(UUID commandId, UUID gameId, DemoTimingConfig timings) implements GameCommand {
    }

    /**
     * Forces the round to start immediately, bypassing programming phase.
     * Only allowed in demo mode, used to demonstrate game mechanics
     *
     * @param commandId unique command identifier
     * @param gameId the game to force start
     * @author William Pii Jæger
     */
    record ForceStartRound(UUID commandId, UUID gameId) implements GameCommand {
    }
}