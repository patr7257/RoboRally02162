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

import static dk.dtu.util.BoardTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Asger Allin Jensen
 */

class AgainCardTest {

    @Test
    void againCardRepeatsMovementFromPreviousRegister() {
        Board board = initBoardWithCheckPoints(5, 5);

        Robot r = new Robot(1, 0, 0, Direction.E);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(
                ProgramCard.move2(),
                ProgramCard.again()
        ));
        game.startRound();

        assertEquals(4, r.getX());
        assertEquals(0, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    @Test
    void againCardRepeatsRotationFromPreviousRegister() {
        Board board = initBoardWithCheckPoints(5, 5);

        Robot r = new Robot(1, 2, 2, Direction.N);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(
                ProgramCard.right(),
                ProgramCard.again(),
                ProgramCard.move1()
        ));
        game.startRound();

        assertEquals(2, r.getX());
        assertEquals(3, r.getY());
        assertEquals(Direction.S, r.getDirection());
    }

    @Test
    void againCardDoesNothingWhenUsedInFirstRegister() {
        Board board = initBoardWithCheckPoints(6, 6);

        Robot r = new Robot(1, 1, 1, Direction.E);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(
                ProgramCard.again(),
                ProgramCard.move2(),
                ProgramCard.again()
        ));
        game.startRound();

        assertEquals(5, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }

    @Test
    void consecutiveAgainCardsOnlyRepeatOriginalAction() {
        Board board = initBoardWithCheckPoints(5, 5);

        Robot r = new Robot(1, 0, 0, Direction.S);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(
                ProgramCard.move1(),
                ProgramCard.again(),
                ProgramCard.again()
        ));
        game.startRound();

        assertEquals(0, r.getX());
        assertEquals(3, r.getY());
        assertEquals(Direction.S, r.getDirection());
    }

    @Test
    void againCardWorksWithBackupMovement() {
        Board board = initBoardWithCheckPoints(5, 5);

        Robot r = new Robot(1, 2, 2, Direction.E);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(
                ProgramCard.back1(),
                ProgramCard.again()
        ));
        game.startRound();

        assertEquals(0, r.getX());
        assertEquals(2, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }
}