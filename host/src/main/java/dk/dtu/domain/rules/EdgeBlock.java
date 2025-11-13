package dk.dtu.domain.rules;

/**
 * @author William Pii Jæger
 */
public record EdgeBlock(Edge edge) implements BlockReason, StopReason {

    /**
     * @author William Pii Jæger
     */
    public EdgeBlock {
        if (edge == null) throw new IllegalArgumentException("edge cannot be null");
    }
}