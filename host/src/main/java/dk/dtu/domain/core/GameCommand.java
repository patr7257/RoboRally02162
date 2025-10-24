package dk.dtu.domain.core;

import dk.dtu.domain.program.ProgramCard;

import java.util.List;
import java.util.UUID;

// Author(s) Weihao Mo, William Pii Jæger

public sealed interface GameCommand permits
        GameCommand.StartProgramming,
        GameCommand.SubmitPrograms,
        GameCommand.EndGame {

    record StartProgramming(UUID commandId, UUID gameId, long windowMs) implements GameCommand {
    }

    record SubmitPrograms(UUID commandId, UUID gameId, PlayerID player,
                          List<ProgramCard> cards) implements GameCommand {
    }

    record EndGame(UUID commandId, UUID gameId) implements GameCommand {
    }
}