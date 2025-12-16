package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.GameObserver;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static dk.dtu.util.BoardTestUtils.*;
import static dk.dtu.util.GameTestSupport.assertPosDir;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Weihao Mo
 */
class GameConveyorTest {

    /**
     * @author Weihao Mo
     */
    @Test
    void greenConveyor_moves_robot() {
        Board board = initBoardWithGreenConveyors(5,5);

        Robot r= new Robot(1,1,0, Direction.E);
        r.loadProgram(List.of());
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);
        game.startRound();

        assertPosDir(r, 2, 4, Direction.S);

    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_moves_robot() {
        Board board = initBoardWithBlueConveyors(10,10);

        Robot r= new Robot(1,1,0, Direction.E);
        r.loadProgram(List.of());
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);
        game.startRound();

        assertPosDir(r, 7, 4, Direction.E);

    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_moves_robot_only_one_space() {
        Board board = initBoardWithBlueConveyors(10,10);

        Robot r= new Robot(1,7,4, Direction.E);
        r.loadProgram(List.of());
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);
        game.startRound();

        assertPosDir(r, 8,4 , Direction.E);

    }

    /**
     * @author Weihao Mo
     */
    @Test
    void greenConveyor_moves_two_robots_to_checkpoints() {
        Board board = initBoardWithGreenConveyorsWithCheckpoints(5,5);

        Robot r1= new Robot(1,0,0, Direction.E);
        Robot r2 = new Robot(2,1,0,Direction.E);
        r1.loadProgram(List.of());
        r2.loadProgram(List.of());
        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);
        AtomicReference<Integer> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(Game game, int winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        game.startRound();

        assertPosDir(r1, 3, 2, Direction.S);
        assertPosDir(r2, 3, 3, Direction.S);
        assertEquals(2,r2.getNextCheckpoint());
        assertEquals(2, game.getWinner().orElse(null));
        assertEquals(2, observedWinner.get());
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_moves_two_robots_to_checkpoints() {
        Board board = initBoardWithBlueConveyorsWithCheckpoints(10,10);

        Robot r1= new Robot(1,0,0, Direction.E);
        Robot r2 = new Robot(2,1,0,Direction.E);
        r1.loadProgram(List.of());
        r2.loadProgram(List.of());
        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);
        AtomicReference<Integer> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(Game game, int winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        game.startRound();

        assertPosDir(r1, 4, 4, Direction.S);
        assertPosDir(r2, 4, 5, Direction.S);
        assertEquals(2,r2.getNextCheckpoint());
        assertEquals(2, game.getWinner().orElse(null));
        assertEquals(2, observedWinner.get());
    }

    /**
     * @author Weihao Mo
     */
   @Test
    void greenConveyor_collision_both_robots_stay_in_place() {
        Board board = initBoardWithGreenConveyorCollision(5, 5);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(2, 2, 0, Direction.W);
        r1.loadProgram(List.of());
        r2.loadProgram(List.of());
        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
        assertPosDir(r2, 2, 0, Direction.W);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_collision_both_robots_stay_in_place() {
        Board board = initBoardWithBlueConveyorCollision(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(2, 4, 0, Direction.W);
        r1.loadProgram(List.of());
        r2.loadProgram(List.of());
        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
        assertPosDir(r2, 4, 0, Direction.W);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_three_robots_collision_all_stay_in_place() {
        Board board = initBoardWithThreeRobotCollision(10, 10);

        Robot r1 = new Robot(1, 0, 1, Direction.E);
        Robot r2 = new Robot(2, 2, 1, Direction.W);
        Robot r3 = new Robot(3, 1, 0, Direction.S);
        r1.loadProgram(List.of());
        r2.loadProgram(List.of());
        r3.loadProgram(List.of());
        List<Robot> robots = List.of(r1, r2, r3);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 1, Direction.E);
        assertPosDir(r2, 2, 1, Direction.W);
        assertPosDir(r3, 1, 0, Direction.S);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_collision_on_second_step() {
        Board board = initBoardWithSecondStepCollision(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(2, 4, 0, Direction.W);
        r1.loadProgram(List.of());
        r2.loadProgram(List.of());
        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
        assertPosDir(r2, 4, 0, Direction.W);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void greenConveyor_get_cancel_move() {
        Board board = initBoardWithGreenConveyorCollision(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(1, 1, 0, Direction.E);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyor_get_cancel_move() {
        Board board = initBoardWithBlueConveyorCancelMove(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(1, 2, 0, Direction.E);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void greenConveyorWithWalls_get_cancel_move() {
        Board board = initBoardWithGreenConveyorAndWalls(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(1, 1, 0, Direction.S);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
        assertPosDir(r2, 1, 0, Direction.S);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blueConveyorWithWalls_get_cancel_move() {
        Board board = initBoardWithBlueConveyorAndWalls(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(1, 1, 0, Direction.S);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 0, Direction.E);
        assertPosDir(r2, 1, 0, Direction.S);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robot_moves_onto_curved_conveyor_by_programming_does_not_rotate() {
        Board board = initBoardWithCurvedConveyorAtDestination(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        r1.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 1, 2, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robot_moves_onto_curved_blue_conveyor_by_programming_does_not_rotate() {
        Board board = initBoardWithCurvedConveyorAtDestinationBlue(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        r1.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 1, 3, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robot_pushed_onto_curved_conveyor_does_not_rotate() {
        Board board = initBoardWithCurvedConveyorAtDestination(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        Robot r2 = new Robot(2, 1, 0, Direction.E);
        r1.loadProgram(List.of(ProgramCard.move1()));
        r2.loadProgram(List.of());
        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 1, 2, Direction.E);
        assertPosDir(r2, 2, 0, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robot_moves_onto_curved_blue_conveyor_by_programming() {
        Board board = initBoardWithBlueConveyors(10, 10);

        Robot r1 = new Robot(1, 4, 0, Direction.W);
        r1.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 8, 4, Direction.S);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robot_moves_onto_curved_green_conveyor_by_programming() {
        Board board = initBoardWithGreenConveyors(10, 10);

        Robot r1 = new Robot(1, 3, 0, Direction.W);
        r1.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 2, 4, Direction.W);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void green_conveyor_from_straight_to_curved() {
        Board board = initBoardWithStraightThenCurvedGreenConveyor(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 1, 1, Direction.S);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blue_conveyor_from_straight_to_curved() {
        Board board = initBoardWithStraightThenCurvedBlueConveyor(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 2, 1, Direction.S);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blue_conveyor__moves_onto_conveyor_by_programming_does_not_rotate() {
        Board board = initBoardWithStraightBlue(10, 10);

        Robot r1 = new Robot(1, 0, 1, Direction.N);
        r1.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 4, 0, Direction.N);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void green_conveyor__moves_onto_conveyor_by_programming_does_not_rotate() {
        Board board = initBoardWithStraightGreen(10, 10);

        Robot r1 = new Robot(1, 0, 1, Direction.N);
        r1.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 4, 0, Direction.N);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void green_conveyor_from_curved_to_curved_does_rotate() {
        Board board = initBoardWithCurvedToCurvedGreenConveyor(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 1, Direction.W);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void blue_conveyor_curved_to_curved_rotates_correctly() {
        Board board = initBoardWithMultipleCurvedBlueConveyors(10, 10);

        Robot r1 = new Robot(1, 0, 0, Direction.E);
        r1.loadProgram(List.of());
        List<Robot> robots = List.of(r1);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        game.startRound();

        assertPosDir(r1, 0, 1, Direction.W);
    }
}
