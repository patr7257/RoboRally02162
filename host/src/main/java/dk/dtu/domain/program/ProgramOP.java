package dk.dtu.domain.program;

// Author(s) Weihao Mo, William Pii Jæger

import dk.dtu.domain.model.Direction;

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
