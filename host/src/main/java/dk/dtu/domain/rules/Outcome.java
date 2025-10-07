package dk.dtu.domain.rules;

import java.util.List;

public sealed interface Outcome
        permits Outcome.Moved, Outcome.Blocked {

    record Moved(List<MoveEvent> moves, List<DestroyEvent> destroys)
            implements Outcome {
        public Moved {
            moves = List.copyOf(moves);
            destroys = List.copyOf(destroys);
        }
    }

    record Blocked(BlockReason reason) implements Outcome {
        public Blocked {
            if (reason == null) {
                throw new IllegalArgumentException("Blocked requires a reason");
            }
        }
    }
}