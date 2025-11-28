package dk.dtu.domain.core.reaction;

import dk.dtu.domain.core.PlayerID;

import java.time.Instant;

public record ReactionRequest<C extends  ReactionChoice>(
    ReactionId id,
    String robotid,
    int registerIndex,
    int opIndex,
    ReactionSpec<C> spec,
    Instant deadline
) {}