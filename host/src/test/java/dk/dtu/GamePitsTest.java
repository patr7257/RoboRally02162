package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.DestroyCause;
import dk.dtu.domain.rules.Outcome;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Pits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dk.dtu.util.BoardTestUtils.initBoardWithRebootToken;
import static dk.dtu.util.BoardTestUtils.initBoardWithRebootTokenAndPits;
import static dk.dtu.util.GameTestSupport.assertMoved;
import static dk.dtu.util.GameTestSupport.assertPosDir;
import static org.junit.jupiter.api.Assertions.*;

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

        game.setRespawnDirection(1, Direction.E);

        game.applyRespawnPhase(r);

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
    void pitsReportsDestroyWithPitsCause() {
        Board board = initBoardWithRebootTokenAndPits(5,5);
        Robot r = new Robot(1, 0, 1, Direction.S);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);

        assertTrue(r.isAlive());

        Tile pitTile = board.getTile(0, 1);
        Pits pits = new Pits();
        pits.onPhase(Phase.ACTIVATE_PITS, pitTile, api);

        Outcome out = api.resolveIntents();
        Outcome.Moved moved = assertMoved(out);

        assertEquals(1, moved.destroys().size());
        assertEquals(DestroyCause.PITS, moved.destroys().getFirst().cause());
        assertEquals(r.getId(), moved.destroys().getFirst().robotId());
    }

}