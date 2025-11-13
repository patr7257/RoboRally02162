package dk.dtu.domain.core;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.infrastructure.dto.ReadinessDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;

import java.util.List;

/**
 * @author William Pii Jæger
 */
public sealed interface GameQuery<T> permits
        GameQuery.GetSnapshot,
        GameQuery.GetHand,
        GameQuery.GetReadiness,
        GameQuery.GetTimeRemaining,
        GameQuery.GetDiscard {

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
}