package dk.dtu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

//for players inside lobby
/**
 * @author Niklas Emil Lysdal
 */
public record LobbyPrivateJson(@JsonProperty("lobbyName") String name, @JsonProperty("lobbyID") String id,
                               @JsonProperty("capacity") int capacity, @JsonProperty("playerCount") int playerCount,
                               @JsonProperty("isRunning") boolean  isRunning, @JsonProperty("readinessMap") Map<String, Boolean> readinessMap)
                            {}

