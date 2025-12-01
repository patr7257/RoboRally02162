package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Weihao Mo
 */
public record DamageDecksDto(@JsonProperty("spamCount") int spamCount, @JsonProperty("trojanHorseCount") int trojanHorseCount, @JsonProperty("wormCount") int wormCount) {
}
