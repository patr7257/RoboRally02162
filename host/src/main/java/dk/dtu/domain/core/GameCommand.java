package dk.dtu.domain.core;

import dk.dtu.domain.program.ProgramCard;

import java.util.List;

// Author(s) Weihao Mo

public sealed interface GameCommand permits GameCommand.SubmitPrograms, GameCommand.StartRound, GameCommand.EndGame {

    record SubmitPrograms(PlayerID player, List<ProgramCard> cards) implements GameCommand {}
    record StartRound() implements GameCommand {}
    record EndGame() implements GameCommand {}
}

