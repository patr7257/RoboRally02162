package dk.dtu.domain.program;



import dk.dtu.domain.model.Direction;
/**
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public sealed interface ProgramOP
        permits ProgramOP.Move, ProgramOP.RotateRight, ProgramOP.RotateLeft, ProgramOP.UTurn {

    default Direction apply(Direction d) {return d; };

    record Move(int steps) implements ProgramOP {
    }

    final class RotateRight implements ProgramOP {
        @Override public Direction apply(Direction d) { return d.turnRight(); }
    }

    final class RotateLeft implements ProgramOP {
        @Override public Direction apply(Direction d) { return d.turnLeft(); }
    }

    final class UTurn implements ProgramOP {
        @Override public Direction apply(Direction d) { return d.turnRight().turnRight(); }
    }
}
