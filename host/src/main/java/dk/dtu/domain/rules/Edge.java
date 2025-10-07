package dk.dtu.domain.rules;

public record Edge(Coord from, Coord to) {
    public Edge {
        if (!from.isAdjacentTo(to)) {
            throw new IllegalArgumentException(
                    "Edge endpoints must be adjacent: " + from + " -> " + to
            );
        }
    }
}