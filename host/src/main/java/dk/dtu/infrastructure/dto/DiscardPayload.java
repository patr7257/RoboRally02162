package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 */
public record DiscardPayload(@JsonProperty("context") String context, List<String> discard) {
}