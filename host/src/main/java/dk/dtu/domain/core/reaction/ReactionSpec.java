package dk.dtu.domain.core.reaction;

import java.util.List;

public record ReactionSpec<C extends ReactionChoice>(
   ReactionKind kind,
   List<C> options,
   C defaultChoice
) {}
