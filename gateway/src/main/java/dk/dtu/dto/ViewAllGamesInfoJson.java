package dk.dtu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Benjamin Benyo Endahl Hansen
 */
public record ViewAllGamesInfoJson(
        @JsonProperty("lobbyName") String lobbyName,
        @JsonProperty("playerCount") int playerCount,
        @JsonProperty("winner") String winner
) {}

