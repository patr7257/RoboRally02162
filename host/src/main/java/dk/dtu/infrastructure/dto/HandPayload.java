package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record HandPayload(@JsonProperty("context") String context, List<String> hand) {


}
