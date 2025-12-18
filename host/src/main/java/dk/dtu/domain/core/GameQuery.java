package dk.dtu.domain.core;

import dk.dtu.domain.model.DamageDecks;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.infrastructure.dto.DamageDecksDto;
import dk.dtu.infrastructure.dto.ReadinessDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;

import java.util.List;
import java.util.Map;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public sealed interface GameQuery<T> permits GameQuery.GetDamageDecks, GameQuery.GetDiscard, GameQuery.GetHand, GameQuery.GetLastMoves, GameQuery.GetReadiness, GameQuery.GetSnapshot, GameQuery.GetTimeRemaining, GameQuery.GetWinner {

    record GetSnapshot() implements GameQuery<SnapshotPayload> {
    }

    record GetHand(int robotId) implements GameQuery<List<ProgramCard>> {
    }

    record GetReadiness() implements GameQuery<ReadinessDto> {
    }

    record GetTimeRemaining() implements GameQuery<Long> {
    }

    record GetDiscard(int robotId) implements GameQuery<List<ProgramCard>> {
    }

    record GetDamageDecks() implements GameQuery<DamageDecks> {
    }

    record GetLastMoves() implements GameQuery<List<Map.Entry<Integer, String>>> {
    }

    record GetWinner() implements  GameQuery<Integer> {}
}