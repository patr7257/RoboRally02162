package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.MoveEvent;
import dk.dtu.domain.rules.Outcome;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;
import java.util.List;

// Author(s) Weihao Mo

public record RebootToken(Direction direction) implements TileEffect {
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        if (phase == Phase.ACTIVATE_REBOOT) {
            int x = tile.getX();
            int y = tile.getY();

            for (Robot robot : api.getDeadRobots()) {
                robot.setPosition(x, y);
                robot.setDirection(direction);
            }

            List<Robot> robotsOnTile = api.getRobotsOnTile(x, y);
            if (robotsOnTile.size() > 1) {
                for (int i = 0; i < robotsOnTile.size() - 1; i++) {
                    Robot r = robotsOnTile.get(i);
                    Outcome result = api.tryMoveOneStep(r.getId(), direction);
                    if (result instanceof Outcome.Moved moved) {
                        for (MoveEvent e : moved.moves()) {
                            Robot movedRobot = robotsOnTile.stream()
                                    .filter(robot -> robot.getId() == e.robotId())
                                    .findFirst()
                                    .orElse(null);
                            if (movedRobot != null) {
                                movedRobot.setPosition(e.to().x(), e.to().y());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_REBOOT);
    }
}
