package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.core.PlayerID;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dk.dtu.util.BoardTestUtils.initBoardWithRebootToken;
import static dk.dtu.util.BoardTestUtils.initBoardWithRebootTokenAndPits;
import static dk.dtu.util.GameTestSupport.assertPosDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GamePitsTest {
    /**
     * @author Weihao Mo
     */
    @Test
    void robotRespawnAfterHitByPits() {
        Board board = initBoardWithRebootTokenAndPits(5,5);
        Robot r= new Robot(1,0,0, Direction.S);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r.loadProgram(List.of(ProgramCard.move1()));

        game.executeRegister(1);

        assertFalse(r.isAlive());

        game.setRespawnDirection(new PlayerID(1), Direction.E);

        game.applyTileEffects(Phase.ACTIVATE_REBOOT);

        assertTrue(r.isAlive());
        assertPosDir(r, 2, 2, Direction.E);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robotRespawnAfterHitByPitsAndThenPushed() {
        Board board = initBoardWithRebootTokenAndPits(5, 5);
        Robot r1 = new Robot(1, 1, 1, Direction.W);
        Robot r2 = new Robot(2, 0, 2, Direction.N);

        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        r1.loadProgram(List.of(ProgramCard.move1()));
        r2.loadProgram(List.of(ProgramCard.move1()));

        game.executeRegister(1);

        assertFalse(r1.isAlive());
        assertFalse(r2.isAlive());

        game.setRespawnDirection(new PlayerID(1), Direction.E);
        game.setRespawnDirection(new PlayerID(2), Direction.E);

        game.applyTileEffects(Phase.ACTIVATE_REBOOT);

        assertTrue(r1.isAlive());
        assertTrue(r2.isAlive());

        assertPosDir(r2, 2, 2, Direction.E);
        assertPosDir(r1, 3, 2, Direction.E);
    }
}
