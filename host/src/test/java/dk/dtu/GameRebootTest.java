package dk.dtu;

import dk.dtu.domain.core.*;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dk.dtu.util.GameTestSupport.*;
import static dk.dtu.util.BoardTestUtils.initBoardWithRebootToken;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Weihao Mo
 */
public class GameRebootTest {

    /**
     * @author Weihao Mo
     */
    @Test
    void robotRespawn() {
        Board board = initBoardWithRebootToken(5,5);
        Robot r= new Robot(1,0,0, Direction.N);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(ProgramCard.move3()));

        game.executeRegister(1);

        assertFalse(r.isAlive());

        game.setRespawnDirection(1, Direction.E);

        game.applyRespawnPhase(r);

        assertTrue(r.isAlive());
        assertPosDir(r, 2, 2, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robotRespawnAndThenPushed() {
        Board board = initBoardWithRebootToken(5, 5);
        Robot r1 = new Robot(1, 0, 0, Direction.N);
        Robot r2 = new Robot(2, 1, 0, Direction.N);

        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r1.loadProgram(List.of(ProgramCard.move3()));
        r2.loadProgram(List.of(ProgramCard.move3()));

        game.executeRegister(1);

        assertFalse(r1.isAlive());
        assertFalse(r2.isAlive());

        game.setRespawnDirection(1, Direction.E);
        game.setRespawnDirection(2, Direction.E);


        game.applyRespawnPhase(r1);
        assertTrue(r1.isAlive());
        game.applyRespawnPhase(r2);
        assertTrue(r2.isAlive());

        assertPosDir(r2, 2, 2, Direction.E);
        assertPosDir(r1, 3, 2, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void threeRobotRespawnAndThenPushed() {
        Board board = initBoardWithRebootToken(5,5);
        Robot r1= new Robot(1,0,0, Direction.N);
        Robot r2= new Robot(2,1,0, Direction.N);
        Robot r3= new Robot(3,2,0, Direction.N);

        List<Robot> robots = List.of(r1,r2,r3);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r1.loadProgram(List.of(ProgramCard.move3()));
        r2.loadProgram(List.of(ProgramCard.move3()));
        r3.loadProgram(List.of(ProgramCard.move3()));

        game.executeRegister(1);

        assertFalse(r1.isAlive());
        assertFalse(r2.isAlive());
        assertFalse(r3.isAlive());

        game.setRespawnDirection(1, Direction.E);
        game.setRespawnDirection(2, Direction.E);
        game.setRespawnDirection(3, Direction.E);


        game.applyRespawnPhase(r1);
        assertTrue(r1.isAlive());
        game.applyRespawnPhase(r2);
        assertTrue(r2.isAlive());
        game.applyRespawnPhase(r3);
        assertTrue(r3.isAlive());

        assertPosDir(r3, 2, 2, Direction.E);
        assertPosDir(r2, 3, 2, Direction.E);
        assertPosDir(r1, 4, 2, Direction.E);
    }
}