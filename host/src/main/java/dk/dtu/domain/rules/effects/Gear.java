package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Rotation;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

/**
 * A gear tile rotates any {@link Robot} standing on it
 * Depending on the gear's configuration,robots will rotate either left or right by 90 degrees.
 *
 * @param rotation the direction this gear rotates robots ({@link Rotation#LEFT}, {@link Rotation#RIGHT}, or {@link Rotation#NONE})
 *
 * @author William Pii Jæger
 */
public record Gear(Rotation rotation) implements TileEffect {

    /**
     * @author Weihao Mo
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        for (Robot robot : api.getRobotsOnTile(tile.getX(), tile.getY())) {
            switch (rotation) {
                case LEFT:
                    robot.setDirection(robot.getDirection().turnLeft());
                    break;
                case RIGHT:
                    robot.setDirection(robot.getDirection().turnRight());
                    break;
                case NONE:
                    return;
            }
        }
    }

    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_GEAR);
    }
}
