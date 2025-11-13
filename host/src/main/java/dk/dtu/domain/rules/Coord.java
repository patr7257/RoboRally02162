package dk.dtu.domain.rules;

/**
 * @author William Pii Jæger
 */
public record Coord(int x, int y) {
    /**
     * @author William Pii Jæger
     */
    public boolean isAdjacentTo(Coord other) {
        int dx = Math.abs(x - other.x);
        int dy = Math.abs(y - other.y);
        return (dx+dy) == 1;
    }
}