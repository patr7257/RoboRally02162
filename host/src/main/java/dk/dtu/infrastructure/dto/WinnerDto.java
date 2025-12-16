package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Weihao Mo
 */
public record WinnerDto(@JsonProperty("winner") Integer winner) {
}
