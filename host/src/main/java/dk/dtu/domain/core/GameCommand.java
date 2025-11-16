package dk.dtu.domain.core;

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
public sealed interface GameCommand permits GameCommand.EndGame, GameCommand.SetRespawnDirection, GameCommand.StartProgramming, GameCommand.SubmitPrograms {

    record StartProgramming(UUID commandId, UUID gameId, long windowMs) implements GameCommand {
    }

    record SubmitPrograms(UUID commandId, UUID gameId, PlayerID player,
                          List<ProgramCard> cards) implements GameCommand {
    }

    record EndGame(UUID commandId, UUID gameId) implements GameCommand {
    }

    record SetRespawnDirection(UUID commandId, UUID gameId, PlayerID player, Direction direction) implements GameCommand{
    }
}