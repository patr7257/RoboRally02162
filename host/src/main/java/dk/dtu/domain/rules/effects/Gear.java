package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Rotation;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

public record Gear(Rotation rotation) implements TileEffect {

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
        return EnumSet.of(Phase.ACTIVATION);
    }
}
