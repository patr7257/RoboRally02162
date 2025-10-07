package dk.dtu.domain.rules;

public record Coord(int x, int y) {
    public boolean isAdjacentTo(Coord other) {
        int dx = Math.abs(x - other.x);
        int dy = Math.abs(y - other.y);
        return (dx+dy) == 1;
    }
}