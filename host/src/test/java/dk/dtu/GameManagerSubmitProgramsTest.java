package dk.dtu;

import dk.dtu.domain.core.*;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.UUID;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;

// Author(s) Weihao Mo

public class GameManagerSubmitProgramsTest extends TestCase {
    public GameManagerSubmitProgramsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GameManagerSubmitProgramsTest.class);
    }

    public void testSubmitProgramsAndRunRound() {
        Board board = initEmptyBoard(3, 3);

        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot(1, 1, 1, Direction.E);

        GameManager manager = new GameManager();
        UUID gid = manager.startGame(board, api, List.of(r));

        CommandResult submitResult = manager.apply(gid, new GameCommand.SubmitPrograms(new PlayerID(1), List.of(ProgramCard.move1())));
        assertEquals("OK", submitResult.reason());

        ProgramOP op = r.getRegisters().getFirst();
        assertTrue(op instanceof ProgramOP.Move);
        assertEquals(1, ((ProgramOP.Move) op).steps());
    }

}
