package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Antenna tile effect that determines robot priority order.
 * <p>
 * The antenna calculates priority based on Manhattan distance from the antenna position,
 * with ties broken by the robot's angular position relative to the antenna's facing direction.
 * </p>
 *
 * @param direction the direction the antenna is facing
 * @author Weihao Mo
 */
public record Antenna(Direction direction) implements TileEffect{

    /**
     * @author Weihao Mo
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        if(phase == Phase.ACTIVATE_ANTENNA) {
            int x = tile.getX();
            int y = tile.getY();

            List<Integer> priorityOrder = api.getRobots().stream().sorted(Comparator
                            .comparingInt((Robot r) -> manhattan(r.getX(), x, r.getY(), y))
                            .thenComparingInt((Robot r) -> tieBreaker(r, x, y, direction)))
                    .map(Robot::getId)
                    .collect(Collectors.toList());
            api.updatePriorityList(priorityOrder);
        }
    }

    private static Integer manhattan(int x1, int x2, int y1, int y2) {
        return Math.abs(x2-x1) + Math.abs(y2-y1);
    }

    private static int tieBreaker(Robot robot, int antennaX, int antennaY, Direction antennaDir) {
        int dx = robot.getX() - antennaX;
        int dy = robot.getY() - antennaY;

        double angleFromNorth = Math.toDegrees(Math.atan2(dx, -dy));
        if (angleFromNorth < 0) angleFromNorth += 360;

        double startAngle = switch (antennaDir) {
            case N -> 0;
            case E -> 90;
            case S -> 180;
            case W -> 270;
        };

        double relativeAngle = angleFromNorth - startAngle;
        if (relativeAngle < 0) relativeAngle += 360;

        return (int) relativeAngle;
    }

    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_ANTENNA);
    }
}
