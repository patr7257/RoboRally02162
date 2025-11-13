package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author William Pii Jæger
 */
public record UpdatePayload(@JsonProperty("context") String context) {

}
