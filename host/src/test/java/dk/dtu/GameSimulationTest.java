package dk.dtu;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;

import java.util.List;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;

// Author(s) William Pii Jæger

public class GameSimulationTest extends TestCase {

    public GameSimulationTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GameSimulationTest.class);
    }

    public void testMoveOneInProgramPhase() {
        Board board = initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(ProgramCard.move1()));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(2, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    public void testMoveTwoInProgramPhase() {
        Board board = initEmptyBoard(5, 5);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 2)));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(3, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    public void testMoveThreeInProgramPhase() {
        Board board = initEmptyBoard(5, 5);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 3)));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(4, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    public void testMoveBackInProgramPhase() {
        Board board = initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, -1)));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(0, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    public void testRotateRightInProgramPhase() {
        Board board = initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.ROTATERIGHT, 0)));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(1, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.S, r.getDirection());
    }

    public void testRotateLeftInProgramPhase() {
        Board board = initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.ROTATELEFT, 0)));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(1, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.N, r.getDirection());
    }

    public void testUTurnInProgramPhase() {
        Board board = initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.UTURN, 0)));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(1, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.W, r.getDirection());
    }
}
