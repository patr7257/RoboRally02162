package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dtu.domain.program.ProgramOP;

import java.util.List;
import java.util.Map;

/**
 * @author Benjamin Benyo Endahl Hansen
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 */
public record LastMovePayload(@JsonProperty("context") String context, String lastMove) {
}