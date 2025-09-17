package dk.dtu;

import dk.dtu.domain.core.*;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;

// Author(s) Weihao Mo

public class GameManagerStartGameTest extends TestCase {
    public GameManagerStartGameTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GameManagerStartGameTest.class);
    }

    public void testStartRoundCommand() {

        Board board = initEmptyBoard(3, 3);
        BoardAPI api = new BoardApiImpl(board);

        Robot r = new Robot("1", 1, 1, Direction.E);
        r.loadProgram(List.of(ProgramCard.move1()));

        GameManager manager = new GameManager();
        GameID gid = manager.startGame(board, api, List.of(r));

        CommandResult result = manager.apply(gid, new GameCommand.StartRound());

        assertEquals("OK", result.reason());
    }

}
