package dk.dtu.domain.core.reaction;

import java.util.UUID;

public record ReactionId(UUID id) {
    public static ReactionId random() { return new ReactionId(UUID.randomUUID()); }
}
