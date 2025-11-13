package dk.dtu.domain.program;

import java.util.List;

/**
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public record ProgramCard(Action action, int steps) {
    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public ProgramCard {
        switch (action) {
            case MOVE -> {
                if (steps < -1 || steps > 3) {
                    throw new IllegalArgumentException("MOVE step must be -1..3");
                }
            }
            case ROTATERIGHT, ROTATELEFT, UTURN -> steps = 0;
        }
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public List<ProgramOP> toOps() {
        return switch (action) {
            case MOVE      -> List.of(new ProgramOP.Move(steps));
            case ROTATERIGHT -> List.of(new ProgramOP.RotateRight());
            case ROTATELEFT  -> List.of(new ProgramOP.RotateLeft());
            case UTURN       -> List.of(new ProgramOP.UTurn());
        };
    }

    /**
     * @author William Pii Jæger
     */
    @Override
    public String toString() {
        return switch (action) {
            case MOVE -> "MOVE" + steps;
            case ROTATELEFT -> "ROTATELEFT";
            case ROTATERIGHT -> "ROTATERIGHT";
            case UTURN -> "UTURN";
        };
    }

    public static ProgramCard move1() { return new ProgramCard(Action.MOVE, 1); }
    public static ProgramCard move2() { return new ProgramCard(Action.MOVE, 2); }
    public static ProgramCard move3() { return new ProgramCard(Action.MOVE, 3); }
    public static ProgramCard back1() { return new ProgramCard(Action.MOVE, -1); }
    public static ProgramCard right() { return new ProgramCard(Action.ROTATERIGHT, 0); }
    public static ProgramCard left()  { return new ProgramCard(Action.ROTATELEFT, 0); }
    public static ProgramCard uturn() { return new ProgramCard(Action.UTURN, 0); }

    public enum Action { MOVE, ROTATERIGHT, ROTATELEFT, UTURN }
}
