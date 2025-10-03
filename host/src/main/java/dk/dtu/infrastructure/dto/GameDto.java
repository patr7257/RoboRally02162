package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record GameDto(@JsonProperty("gameID") UUID gameID, @JsonProperty("winner") Integer winner) {
}
