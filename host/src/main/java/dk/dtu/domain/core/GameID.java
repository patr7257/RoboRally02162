package dk.dtu.domain.core;

import java.util.UUID;

// Author(s) Weihao Mo

public record GameID(UUID value) {
    public static GameID newID() {
        return new GameID(UUID.randomUUID());
    }
}

