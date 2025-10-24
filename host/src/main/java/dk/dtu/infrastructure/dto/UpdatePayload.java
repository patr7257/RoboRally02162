package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdatePayload(@JsonProperty("context") String context) {

}
