package dk.dtu;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Antenna;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;
import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * @author Weihao Mo
 */
public class AntennaTest {
    private Tile antennaTileAt(Board b, int x, int y) {
        return b.getTile(x, y);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void orders_by_distance() {
        Board b = initEmptyBoard(9, 9);
        Antenna antenna = new Antenna(Direction.N);
        Tile ant = antennaTileAt(b, 3, 3);

        Robot r1 = new Robot(1, 4, 3, Direction.E);
        Robot r2  = new Robot(2, 4, 4, Direction.E);
        Robot r3 = new Robot(3, 3, 7, Direction.E);
        Robot r4 = new Robot(4, 0, 4, Direction.E);

        BoardAPI api = new BoardApiImpl(b, List.of(r1, r2, r3, r4));

        antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

        List<Integer> order = api.getRobotsByPriority().stream().map(Robot::getId).toList();

        assertEquals(List.of(1, 2, 3, 4), order);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void orders_by_clockwise() {
        Board b = initEmptyBoard(7, 7);
        Antenna antenna = new Antenna(Direction.N);
        Tile ant = antennaTileAt(b, 3, 3);

        Robot r = new Robot(1, 3, 2, Direction.E);
        Robot r2 = new Robot(2, 3, 4, Direction.E);
        Robot r3 = new Robot(3, 4, 3, Direction.E);
        Robot r4 = new Robot(4, 2, 3, Direction.E);
        Robot r5 = new Robot(5, 6, 3, Direction.E);

        BoardAPI api = new BoardApiImpl(b, List.of(r, r2, r3, r4, r5));

        antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

        List<Robot> ordered = api.getRobotsByPriority();
        assertEquals(List.of(1, 3, 2, 4, 5),
                ordered.stream().map(Robot::getId).toList());
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void diagonals_are_ordered_by_true_angle_clockwise() {
        Board b = initEmptyBoard(7, 7);
        Antenna antenna = new Antenna(Direction.N);
        Tile ant = antennaTileAt(b, 3, 3);

        Robot r1 = new Robot(1, 4, 2, Direction.E);
        Robot r2 = new Robot(2, 4, 4, Direction.E);
        Robot r3 = new Robot(3, 2, 4, Direction.E);
        Robot r4 = new Robot(4, 2, 2, Direction.E);

        BoardAPI api = new BoardApiImpl(b, List.of(r1, r2, r3, r4));
        antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

        List<Integer> order = api.getRobotsByPriority().stream().map(Robot::getId).toList();
        assertEquals(List.of(1, 2, 3, 4), order);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void mixed_distance_tie_east() {
        Board b = initEmptyBoard(9, 9);
        Antenna antenna = new Antenna(Direction.E);
        Tile ant = b.getTile(4,4);

        Robot r1 = new Robot(1, 5, 4, Direction.N);
        Robot r2 = new Robot(2, 4, 3, Direction.N);

        Robot r3 = new Robot(3, 4, 6, Direction.N);
        Robot r4 = new Robot(4, 7, 4, Direction.N);

        BoardAPI api = new BoardApiImpl(b, List.of(r1,r2,r3,r4));

        antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

        List<Integer> order = api.getRobotsByPriority().stream().map(Robot::getId).toList();

        assertEquals(List.of(1, 2, 3, 4), order);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void mixed_distance_tie_south() {
        Board b = initEmptyBoard(9, 9);
        Antenna antenna = new Antenna(Direction.S);
        Tile ant = b.getTile(4, 4);

        Robot r1 = new Robot(1, 4, 5, Direction.N);

        Robot r2 = new Robot(2, 5, 4, Direction.N);

        Robot r3 = new Robot(3, 3, 4, Direction.N);

        Robot r4 = new Robot(4, 4, 7, Direction.N);

        BoardAPI api = new BoardApiImpl(b, List.of(r1, r2, r3, r4));

        antenna.onPhase(Phase.ACTIVATE_ANTENNA, ant, api);

        List<Integer> order = api.getRobotsByPriority().stream().map(Robot::getId).toList();

        assertEquals(List.of(1, 3, 2, 4), order);
    }
}
