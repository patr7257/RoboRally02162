package dk.dtu.domain.program;

import java.util.List;

// Author(s) Weihao Mo, William Pii Jæger

public record ProgramCard(Action action, int steps) {
    public ProgramCard {
        if (action == Action.MOVE && (steps < 1 || steps > 3)) {
            throw new IllegalArgumentException("MOVE step must be 1..3");
        }
    }

    public List<ProgramOP> toOps() {
        return switch (action) {
            case MOVE -> List.of(new ProgramOP.Move(steps));
        };
    }

    public static ProgramCard move1() {
        return new ProgramCard(Action.MOVE, 1);
    }

    public enum Action {MOVE}
}
