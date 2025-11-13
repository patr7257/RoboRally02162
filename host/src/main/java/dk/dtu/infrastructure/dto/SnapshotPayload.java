package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public record SnapshotPayload(@JsonProperty("game") GameDto game, BoardDto board, List<RobotDto> robots) {
}
