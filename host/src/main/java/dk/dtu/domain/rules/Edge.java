package dk.dtu.domain.rules;

/**
 * @author William Pii Jæger
 */
public record Edge(Coord from, Coord to) {

    /**
     * @author William Pii Jæger
     */
    public Edge {
        if (!from.isAdjacentTo(to)) {
            throw new IllegalArgumentException(
                    "Edge endpoints must be adjacent: " + from + " -> " + to
            );
        }
    }
}