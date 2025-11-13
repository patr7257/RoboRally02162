package dk.dtu.domain.rules;

import java.util.List;

/**
 * It says a block reason that occurs when multiple robots
 * are pushing against each other and the chain is stopped by a wall, board edge, or other immovable obstacle.
 *
 * @author William Pii Jæger
 */
public record RobotChainImmovable(List<Integer> chain, StopReason stop) implements BlockReason {

    /**
     * @author William Pii Jæger
     */
    public RobotChainImmovable {
        chain = List.copyOf(chain);
        if (chain.isEmpty()) throw new IllegalArgumentException("chain must be non-empty");
        if (stop == null) throw new IllegalArgumentException("stop cannot be null");
    }
}