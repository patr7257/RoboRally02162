package dk.dtu.domain.rules;

import java.util.List;

public record RobotChainImmovable(List<Integer> chain, StopReason stop) implements BlockReason {
    public RobotChainImmovable {
        chain = List.copyOf(chain);
        if (chain.isEmpty()) throw new IllegalArgumentException("chain must be non-empty");
        if (stop == null) throw new IllegalArgumentException("stop cannot be null");
    }
}