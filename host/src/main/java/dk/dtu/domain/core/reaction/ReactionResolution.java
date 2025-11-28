package dk.dtu.domain.core.reaction;

public record ReactionResolution<C extends ReactionChoice>(
   ReactionId id,
   C choice
) {}
