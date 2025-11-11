package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record DiscardPayload(@JsonProperty("context") String context, List<String> discard) {
}