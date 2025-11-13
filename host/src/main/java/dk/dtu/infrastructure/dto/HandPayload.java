package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author William Pii Jæger
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 */
public record HandPayload(@JsonProperty("context") String context, List<String> hand) {
}
