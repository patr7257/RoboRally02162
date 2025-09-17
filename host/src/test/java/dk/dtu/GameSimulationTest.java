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

        Robot r = new Robot("1", 1, 1, Direction.E);
        r.loadProgram(List.of(ProgramCard.move1()));

        Game game = new Game(board, api, List.of(r));
        game.startRound();

        assertEquals(2, r.getX());
        assertEquals(1, r.getY());
        assertEquals(Direction.E, r.getDirection());
    }
}
