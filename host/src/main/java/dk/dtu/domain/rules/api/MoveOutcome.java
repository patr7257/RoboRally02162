package dk.dtu.domain.rules.api;

// Author(s) William Pii Jæger

public record MoveOutcome(boolean moved, int toX, int toY, String reason) {
    public static MoveOutcome blocked(String reason) {
        return new MoveOutcome(false, -1, -1, reason);
    }

    public static MoveOutcome movedTo(int x, int y) {
        return new MoveOutcome(true, x, y, "OK");
    }
}
