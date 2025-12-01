package dk.dtu.domain.program;



import dk.dtu.domain.core.reaction.ReactionKind;
import dk.dtu.domain.model.Direction;
/**
 * @author William Pii Jæger
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 */
public sealed interface ProgramOP
        permits ProgramOP.Again, ProgramOP.Move, ProgramOP.Reaction, ProgramOP.RotateLeft, ProgramOP.RotateRight, ProgramOP.Spam, ProgramOP.TrojanHorse, ProgramOP.UTurn, ProgramOP.Worm {

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

    record Spam() implements ProgramOP {
    }

    record TrojanHorse() implements ProgramOP {
    }

    record Worm() implements ProgramOP {
    }

    final class Again implements ProgramOP {
    }

    record Reaction(ReactionKind kind) implements ProgramOP {}
}
