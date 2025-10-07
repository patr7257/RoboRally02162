package dk.dtu.domain.rules;

public record EdgeBlock(Edge edge) implements BlockReason, StopReason {
    public EdgeBlock {
        if (edge == null) throw new IllegalArgumentException("edge cannot be null");
    }
}