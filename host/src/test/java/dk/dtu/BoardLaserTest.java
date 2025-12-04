package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.core.PlayerID;
import dk.dtu.domain.model.*;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Antenna;
import dk.dtu.domain.rules.effects.BoardLaser;
import dk.dtu.domain.rules.effects.Walls;
import org.junit.jupiter.api.Test;

import java.util.*;

import static dk.dtu.util.BoardTestUtils.*;
import static dk.dtu.util.GameTestSupport.assertPosDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Patrick Røbel
 */
public class BoardLaserTest {
    /**
     * @author Patrick Røbel
     */
    @Test
    void boardLaserOnRobotDealsDamage() {
        Board board = initBoardWithBoardLasers(10, 10);
        Robot robot = new Robot(1, 2, 2, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        List<ProgramCard> discardBefore = game.getRobotDiscard(1);
        assertEquals(0, discardBefore.size());
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(1, discardAfter.size());
        assertTrue(discardAfter.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        
        assertPosDir(robot, 2, 2, Direction.N);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void powerEqualsSpamCardCount() {
        Board board = initBoardWithBoardLasers(10, 10);
        Robot robot = new Robot(1, 3, 3, Direction.E);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        List<ProgramCard> discardBefore = game.getRobotDiscard(1);
        assertEquals(0, discardBefore.size());
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(3, discardAfter.size());
        
        long spamCount = discardAfter.stream()
                                     .filter(c -> c.action() == ProgramCard.Action.SPAM)
                                     .count();
        assertEquals(3, spamCount);
        
        assertPosDir(robot, 3, 3, Direction.E);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void laserHitsWallAndStops() {
        Board board = initBoardWithBoardLasers(10, 10);
        Robot robot = new Robot(1, 1, 3, Direction.S);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(0, discardAfter.size());
        
        assertPosDir(robot, 1, 3, Direction.S);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void laserHitsMaxOneRobot() {
        Board board = initBoardWithBoardLasers(10, 10);
        Robot robot1 = new Robot(1, 4, 5, Direction.W);
        Robot robot2 = new Robot(2, 6, 5, Direction.W);
        List<Robot> robots = List.of(robot1, robot2);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        List<ProgramCard> discard1Before = game.getRobotDiscard(1);
        List<ProgramCard> discard2Before = game.getRobotDiscard(2);
        assertEquals(0, discard1Before.size());
        assertEquals(0, discard2Before.size());
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discard1After = game.getRobotDiscard(1);
        List<ProgramCard> discard2After = game.getRobotDiscard(2);
        
        assertEquals(2, discard1After.size());
        assertEquals(0, discard2After.size());
        
        long spam1Count = discard1After.stream()
                                       .filter(c -> c.action() == ProgramCard.Action.SPAM)
                                       .count();
        assertEquals(2, spam1Count);
        
        assertPosDir(robot1, 4, 5, Direction.W);
        assertPosDir(robot2, 6, 5, Direction.W);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void robotOnLaserTileIsHit() {
        Board board = initBoardWithBoardLasers(10, 10);
        // Robot standing directly on laser tile at (2, 1)
        Robot robot = new Robot(1, 2, 1, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        List<ProgramCard> discardBefore = game.getRobotDiscard(1);
        assertEquals(0, discardBefore.size());
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(1, discardAfter.size());
        assertTrue(discardAfter.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        
        assertPosDir(robot, 2, 1, Direction.N);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void robotTakesDamageEachRound() {
        Board board = initBoardWithBoardLasers(10, 10);
        Robot robot = new Robot(1, 2, 2, Direction.E);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        // Round 1
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        List<ProgramCard> discardAfterRound1 = game.getRobotDiscard(1);
        assertEquals(1, discardAfterRound1.size());
        
        // Round 2 - robot still in same position
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        List<ProgramCard> discardAfterRound2 = game.getRobotDiscard(1);
        assertEquals(2, discardAfterRound2.size());
        
        // Round 3
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        List<ProgramCard> discardAfterRound3 = game.getRobotDiscard(1);
        assertEquals(3, discardAfterRound3.size());
        
        // All should be SPAM cards
        long spamCount = discardAfterRound3.stream()
                                           .filter(c -> c.action() == ProgramCard.Action.SPAM)
                                           .count();
        assertEquals(3, spamCount);
        
        assertPosDir(robot, 2, 2, Direction.E);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void laserBlockedByAntenna() {
        Tile[][] tiles = initEmptyCells(10, 10);
        
        // Laser at (5, 5) facing East
        tiles[5][5].setEffects(List.of(new BoardLaser(Direction.E, 2)));
        
        // Antenna at (7, 5) - should block laser
        tiles[7][5].setEffects(List.of(new Antenna(Direction.N)));
        
        Board board = new Board(10, 10, tiles);
        
        // Robot at (8, 5) - should NOT be hit due to antenna blocking
        Robot robot = new Robot(1, 8, 5, Direction.W);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(0, discardAfter.size(), "Robot should not be hit - antenna blocks laser");
        
        assertPosDir(robot, 8, 5, Direction.W);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void wallAndRobotSameTileWallBlocksLaserWhenFacingOpposite() {
        Tile[][] tiles = initEmptyCells(10, 10);
        
        // Laser at (3, 5) facing East
        tiles[3][5].setEffects(List.of(new BoardLaser(Direction.E, 1)));
        
        // Robot at (5, 5) with wall facing West (blocks laser coming from West/East direction)
        tiles[5][5].setEffects(List.of(new Walls(EnumSet.of(Direction.W))));
        
        Board board = new Board(10, 10, tiles);
        Robot robot = new Robot(1, 5, 5, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(0, discardAfter.size(), "Robot should NOT be hit - wall blocks laser");
        
        assertPosDir(robot, 5, 5, Direction.N);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void wallAndRobotSameTileWallBlocksLaserWhenFacingNonOpposite() {
        Tile[][] tiles = initEmptyCells(10, 10);
        
        // Laser at (3, 5) facing East
        tiles[3][5].setEffects(List.of(new BoardLaser(Direction.E, 1)));
        
        // Robot at (5, 5) with walls facing North, South, and East (all non-opposite to laser direction)
        tiles[5][5].setEffects(List.of(new Walls(EnumSet.of(Direction.N, Direction.S, Direction.E))));
        
        Board board = new Board(10, 10, tiles);
        Robot robot = new Robot(1, 5, 5, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(1, discardAfter.size(), "Robot SHOULD be hit - walls do not block laser");
        assertTrue(discardAfter.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        
        assertPosDir(robot, 5, 5, Direction.N);
    }
}