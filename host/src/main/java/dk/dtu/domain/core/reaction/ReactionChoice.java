package dk.dtu.domain.core.reaction;

public sealed interface ReactionChoice permits ReactionChoice.SandBoxChoice, ReactionChoice.SpeedChoice, ReactionChoice.WeaselChoice {
    enum SandBoxChoice implements ReactionChoice { MOVE1, MOVE2, MOVE3, BACKUP, LEFT, RIGHT, UTURN }
    enum WeaselChoice  implements ReactionChoice { LEFT, RIGHT, UTURN }
    enum SpeedChoice   implements ReactionChoice { MOVE3 }
}
