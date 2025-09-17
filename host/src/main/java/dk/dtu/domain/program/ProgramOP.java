package dk.dtu.domain.program;

// Author(s) Weihao Mo, William Pii Jæger

public sealed interface ProgramOP permits ProgramOP.Move {
    record Move(int steps) implements ProgramOP {
    }
}
