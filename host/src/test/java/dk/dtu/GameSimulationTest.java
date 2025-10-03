package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Author(s) William Pii Jæger

class GameSimulationTest {
    @Test
    void testMoveOneInProgramPhase() {
        Board board = initEmptyBoard(3, 3);


        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(2, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    @Test
    void testMoveTwoInProgramPhase() {
        Board board = initEmptyBoard(5, 5);


        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 2)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(3, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    @Test
    void testMoveThreeInProgramPhase() {
        Board board = initEmptyBoard(5, 5);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 3)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(4, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    @Test
    void testMoveBackInProgramPhase() {
        Board board = initEmptyBoard(3, 3);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, -1)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(0, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    @Test
    void testRotateRightInProgramPhase() {
        Board board = initEmptyBoard(3, 3);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.ROTATERIGHT, 0)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(1, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.S, r.getDirection());
    }

    @Test
    void testRotateLeftInProgramPhase() {
        Board board = initEmptyBoard(3, 3);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.ROTATELEFT, 0)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(1, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.N, r.getDirection());
    }

    @Test
    void testUTurnInProgramPhase() {
        Board board = initEmptyBoard(3, 3);

        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.UTURN, 0)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(1, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.W, r.getDirection());
    }
}
