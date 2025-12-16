package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

/**
 * Checkpoint tile effect that registers robots when robots land on it after registration is complete
 * Robots can only register it if it matches the next checkpoint they are supposed to reach.
 *
 * @param number the number checkpoint is beholding
 * @author Weihao Mo
 */
public record Checkpoint(int number) implements TileEffect {

    /**
     * @author Weihao Mo
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
            int x = tile.getX();
            int y = tile.getY();
            for (Robot robot : api.getRobotsOnTile(x, y)) {
                robot.advanceCheckpointIfMatches(number);
            }
    }

    /**
     * @author Weihao Mo
     */
    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_CHECKPOINTS);
    }
}
