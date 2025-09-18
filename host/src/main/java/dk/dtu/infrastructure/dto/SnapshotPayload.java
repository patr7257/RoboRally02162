package dk.dtu.infrastructure.dto;

import java.util.List;

public record SnapshotPayload(BoardDto board, List<RobotDto> robots) {
}
