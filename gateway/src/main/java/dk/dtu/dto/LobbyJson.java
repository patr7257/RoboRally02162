package dk.dtu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LobbyJson(@JsonProperty("lobbyID") String id) {}
