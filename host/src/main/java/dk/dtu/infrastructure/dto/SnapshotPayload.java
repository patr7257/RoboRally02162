package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SnapshotPayload(@JsonProperty("game") GameDto game, BoardDto board, List<RobotDto> robots) {
}
