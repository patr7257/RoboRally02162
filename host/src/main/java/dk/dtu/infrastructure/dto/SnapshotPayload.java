package dk.dtu.infrastructure.dto;

import java.util.List;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public record SnapshotPayload(BoardDto board, List<RobotDto> robots) {
}
