package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Benjamin Benyo Endahl Hansen
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 */
public record LastMovesPayload(@JsonProperty("context") String context, String lastMoves) {
}