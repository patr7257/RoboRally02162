package dk.dtu.domain.rules;

import dk.dtu.domain.model.Direction;
import dk.dtu.domain.rules.api.BoardApiImpl;

import java.util.List;

/**
 * Represents the result of a movement attempt in the game.
 * Can either be a successful move with associated events or a blocked move with a reason.
 * This sealed interface ensures all possible outcomes are handled
 *
 * @author William Pii Jæger
 * @see BoardApiImpl#resolveIntents()
 * @see BoardApiImpl#tryMoveOneStep(int, Direction)
 */
public sealed interface Outcome
        permits Outcome.Moved, Outcome.Blocked {

    /**
     * @author William Pii Jæger
     */
    record Moved(List<MoveEvent> moves, List<DestroyEvent> destroys)
            implements Outcome {
        public Moved {
            moves = List.copyOf(moves);
            destroys = List.copyOf(destroys);
        }
    }

    /**
     * @author William Pii Jæger
     */
    record Blocked(BlockReason reason) implements Outcome {
        public Blocked {
            if (reason == null) {
                throw new IllegalArgumentException("Blocked requires a reason");
            }
        }
    }
}