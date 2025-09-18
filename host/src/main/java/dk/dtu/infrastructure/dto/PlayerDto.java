package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerDto(@JsonProperty("playerID") int playerID) {
}
