package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public record PlayerDto(@JsonProperty("playerID") int playerID) {
}
