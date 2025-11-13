package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.List;
import java.util.stream.Stream;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;
import static dk.dtu.util.GameTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author William Pii Jæger
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GameSimulationTest {

    /**
     * @author William Pii Jæger
     */
    @ParameterizedTest(name = "MOVE {0} from (1,1,E) -> expected x={1}")
    @CsvSource({
            "1, 2",
            "2, 3",
            "3, 4",
            "-1, 0"
    })
    void program_MOVE_steps(int steps, int expectedX) {
        Board b = initEmptyBoard(5, 5);
        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, steps)));
        BoardAPI api = new BoardApiImpl(b, List.of(r));

        // For now, we have to use startRound, as it keeps track of the amount of steps to take
        new Game(b, api, List.of(r)).startRound();

        assertPosDir(r, expectedX, 1, Direction.E);
    }

    static Stream<Arguments> rotations() {
        return Stream.of(
                Arguments.of(ProgramCard.Action.ROTATERIGHT, Direction.E, Direction.S),
                Arguments.of(ProgramCard.Action.ROTATELEFT , Direction.E, Direction.N),
                Arguments.of(ProgramCard.Action.UTURN      , Direction.E, Direction.W)
        );
    }

    /**
     * @author William Pii Jæger
     */
    @ParameterizedTest(name = "{0} from facing {1} -> {2}")
    @MethodSource("rotations")
    void program_ROTATE_variants(ProgramCard.Action action, Direction start, Direction expected) {
        Board b = initEmptyBoard(3, 3);
        Robot r = new Robot(1, 1, 1, start);
        r.loadProgram(List.of(new ProgramCard(action, 0)));
        BoardAPI api = new BoardApiImpl(b, List.of(r));

        new Game(b, api, List.of(r)).startRound();

        assertPosDir(r, 1, 1, expected);
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void push_chain_tail_falls_off_right_edge() {
        Board b = initEmptyBoard(3, 3);
        List<Robot> rs = lineRobots(1, 0, 0, 3, Direction.E);
        BoardAPI api = new BoardApiImpl(b, rs);

        Outcome out = api.tryMoveOneStep(1, Direction.E);

        Outcome.Moved moved = assertMoved(out);
        assertEquals(1, moved.destroys().size(), "exactly one destroy (tail falls)");
        assertDestroy(moved, 0, 3, new Coord(3, 0));
        assertEquals(DestroyCause.FELL_OFF, moved.destroys().getLast().cause());

        assertEquals(2, moved.moves().size(), "R2 then R1");
        assertMove(moved, 0, 2, new Coord(1, 0), new Coord(2, 0));
        assertMove(moved, 1, 1, new Coord(0, 0), new Coord(1, 0));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void wall_on_same_tile_blocks_exit_edge() {
        Board b = initEmptyBoard(3, 3);
        walls(b, 0, 0, Direction.E);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r1));

        Outcome out = api.tryMoveOneStep(1, Direction.E);
        assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void wall_on_adjacent_tile_blocks_entry_edge() {
        Board b = initEmptyBoard(3, 3);
        walls(b, 1, 0, Direction.W);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r1));

        Outcome out = api.tryMoveOneStep(1, Direction.E);
        assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void push_chain_blocked_by_wall_on_target_edge() {
        Board b = initEmptyBoard(3, 3);
        walls(b, 1, 0, Direction.E);

        List<Robot> rs = lineRobots(1, 0, 0, 2, Direction.E);
        BoardAPI api = new BoardApiImpl(b, rs);

        Outcome out = api.tryMoveOneStep(1, Direction.E);

        assertChainBlockedByEdge(out, List.of(2), new Edge(new Coord(1, 0), new Coord(2, 0)));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void walls_on_board_edge() {
        Board b = initEmptyBoard(1, 1);
        walls(b, 0, 0, Direction.E);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r1));

        Outcome out = api.tryMoveOneStep(1, Direction.E);
        assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void fall_off_edge() {
        Board b = initEmptyBoard(1, 1);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r1));
        Outcome out = api.tryMoveOneStep(1, Direction.E);

        Outcome.Moved moved = assertMoved(out);
        assertDestroy(moved, 0, 1, new Coord(1, 0));
        assertEquals(DestroyCause.FELL_OFF, moved.destroys().getLast().cause());
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void walls_on_both_tiles_but_non_blocking_across_E() {
        Board b = initEmptyBoard(3, 3);
        walls(b, 0, 0, Direction.S, Direction.N, Direction.W);
        walls(b, 1, 0, Direction.S, Direction.N, Direction.E);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r1));

        Outcome out = api.tryMoveOneStep(1, Direction.E);

        Outcome.Moved moved = assertMoved(out);
        assertTrue(moved.destroys().isEmpty(), "no robots destroyed");
        assertEquals(1, moved.moves().size(), "one move event expected");
        assertMove(moved, 0, 1, new Coord(0, 0), new Coord(1, 0));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void program_MOVE_minus1_blocked_by_wall_behind() {
        Board b = initEmptyBoard(3, 1);
        walls(b, 0, 0, Direction.E);

        Robot r = new Robot(1, 1, 0, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, -1)));
        BoardAPI api = new BoardApiImpl(b, List.of(r));

        new Game(b, api, List.of(r)).startRound();
        assertPosDir(r, 1, 0, Direction.E);
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void backward_step_off_left_edge_is_destroyed() {
        Board b = initEmptyBoard(1, 1);
        Robot r = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r));

        Outcome out = api.tryMoveOneStep(1, Direction.W);
        Outcome.Moved moved = assertMoved(out);

        assertEquals(0, moved.moves().size(), "no moves when immediately falling");
        assertEquals(1, moved.destroys().size());
        assertDestroy(moved, 0, 1, new Coord(-1, 0));
        assertEquals(DestroyCause.FELL_OFF, moved.destroys().getLast().cause());
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void push_chain_simple_two_robots() {
        Board b = initEmptyBoard(3, 1);
        List<Robot> rs = lineRobots(1, 0, 0, 2, Direction.E);
        BoardAPI api = new BoardApiImpl(b, rs);

        Outcome out = api.tryMoveOneStep(1, Direction.E);
        Outcome.Moved moved = assertMoved(out);

        assertTrue(moved.destroys().isEmpty());
        assertEquals(2, moved.moves().size());
        assertMove(moved, 0, 2, new Coord(1, 0), new Coord(2, 0));
        assertMove(moved, 1, 1, new Coord(0, 0), new Coord(1, 0));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void push_chain_blocked_by_wall_further_down_chain() {
        Board b = initEmptyBoard(4, 1);
        walls(b, 2, 0, Direction.E);

        List<Robot> rs = lineRobots(1, 0, 0, 3, Direction.E);
        BoardAPI api = new BoardApiImpl(b, rs);

        Outcome out = api.tryMoveOneStep(1, Direction.E);
        assertChainBlockedByEdge(out, List.of(2, 3), new Edge(new Coord(2, 0), new Coord(3, 0)));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void two_walls_on_crossing_edge_also_blocks() {
        Board b = initEmptyBoard(3, 1);
        walls(b, 0, 0, Direction.E);
        walls(b, 1, 0, Direction.W);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        BoardAPI api = new BoardApiImpl(b, List.of(r1));

        Outcome out = api.tryMoveOneStep(1, Direction.E);
        assertEdgeBlock(out, new Edge(new Coord(0, 0), new Coord(1, 0)));
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void program_MOVE_zero_is_noop() {
        Board b = initEmptyBoard(3, 3);
        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 0)));
        BoardAPI api = new BoardApiImpl(b, List.of(r));

        new Game(b, api, List.of(r)).startRound();
        assertPosDir(r, 1, 1, Direction.E);
    }
}