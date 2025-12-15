package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.*;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.RobotLaser;
import org.junit.jupiter.api.Test;

import java.util.*;

import static dk.dtu.util.BoardTestUtils.*;
import static dk.dtu.util.GameTestSupport.assertPosDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Patrick Røbel
 */
public class RobotLaserTest {

    /**
     * @author Patrick Røbel
     */
    @Test
    void twoRobotsFacingEachOtherBothDealDamage() {
        Board board = initBoardWithRobotLasers(10, 10);
        
        Robot robot1 = new Robot(1, 2, 5, Direction.E);
        Robot robot2 = new Robot(2, 5, 5, Direction.W);
        List<Robot> robots = List.of(robot1, robot2);
        
        BoardAPI api = new BoardApiImpl(board, robots);
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        assertEquals(0, game.getRobotDiscard(1).size());
        assertEquals(0, game.getRobotDiscard(2).size());
        
        game.applyTileEffects(Phase.ACTIVATE_ROBOT_LASERS);
        
        List<ProgramCard> discard1 = game.getRobotDiscard(1);
        List<ProgramCard> discard2 = game.getRobotDiscard(2);
        
        assertEquals(1, discard1.size());
        assertEquals(1, discard2.size());
        assertTrue(discard1.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        assertTrue(discard2.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        
        assertPosDir(robot1, 2, 5, Direction.E);
        assertPosDir(robot2, 5, 5, Direction.W);
    }
    
    /**
     * @author Patrick Røbel
     */
    @Test
    void robotLaserDoesNotHitItself() {
        Board board = initBoardWithRobotLasers(10, 10);
        
        Robot robot = new Robot(1, 3, 3, Direction.N);
        List<Robot> robots = List.of(robot);
        
        BoardAPI api = new BoardApiImpl(board, robots);
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        assertEquals(0, game.getRobotDiscard(1).size());
        
        game.applyTileEffects(Phase.ACTIVATE_ROBOT_LASERS);
        
        assertEquals(0, game.getRobotDiscard(1).size());
        
        assertPosDir(robot, 3, 3, Direction.N);
    }
    
    /**
     * @author Patrick Røbel
     */
    @Test
    void robotLaserEffectsNotPresentBeforeActivation() {
        Board board = initBoardWithRobotLasers(10, 10);
        
        Robot robot1 = new Robot(1, 2, 2, Direction.E);
        Robot robot2 = new Robot(2, 5, 5, Direction.W);
        List<Robot> robots = List.of(robot1, robot2);
        
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);
        
        Tile tile1 = board.getTile(2, 2);
        Tile tile2 = board.getTile(5, 5);
        
        long robotLaserCount1 = tile1.getEffects().stream()
            .filter(e -> e instanceof RobotLaser)
            .count();
        long robotLaserCount2 = tile2.getEffects().stream()
            .filter(e -> e instanceof RobotLaser)
            .count();
        
        assertEquals(0, robotLaserCount1);
        assertEquals(0, robotLaserCount2);
    }
    
    /**
     * @author Patrick Røbel
     */
    @Test
    void robotLaserEffectsAddedDuringActivationAndRemovedAfter() {
        Board board = initBoardWithRobotLasers(10, 10);
        
        Robot robot1 = new Robot(1, 2, 3, Direction.E);
        Robot robot2 = new Robot(2, 5, 3, Direction.W);
        List<Robot> robots = List.of(robot1, robot2);
        
        BoardAPI api = new BoardApiImpl(board, robots);
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        Tile tile1 = board.getTile(2, 3);
        Tile tile2 = board.getTile(5, 3);
        
        long beforeCount1 = tile1.getEffects().stream().filter(e -> e instanceof RobotLaser).count();
        long beforeCount2 = tile2.getEffects().stream().filter(e -> e instanceof RobotLaser).count();
        assertEquals(0, beforeCount1);
        assertEquals(0, beforeCount2);
        
        game.runPhase(Phase.ACTIVATION, () -> {});
        
        long afterCount1 = tile1.getEffects().stream().filter(e -> e instanceof RobotLaser).count();
        long afterCount2 = tile2.getEffects().stream().filter(e -> e instanceof RobotLaser).count();
        assertEquals(0, afterCount1);
        assertEquals(0, afterCount2);
        
        assertEquals(1, game.getRobotDiscard(1).size());
        assertEquals(1, game.getRobotDiscard(2).size());
        
        assertTrue(game.getRobotDiscard(1).stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        assertTrue(game.getRobotDiscard(2).stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
    }
    
    /**
     * @author Patrick Røbel
     */
    @Test
    void deadRobotsDoNotGetLaserEffects() {
        Board board = initBoardWithRobotLasers(10, 10);
        
        Robot robot1 = new Robot(1, 2, 2, Direction.E);
        Robot robot2 = new Robot(2, 5, 5, Direction.W);
        
        robot2.setDead();
        
        List<Robot> robots = List.of(robot1, robot2);
        
        BoardAPI api = new BoardApiImpl(board, robots);
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        game.runPhase(Phase.ACTIVATION, () -> {});
        
        assertEquals(0, game.getRobotDiscard(1).size());
    }
    
    /**
     * @author Patrick Røbel
     */
    @Test
    void robotLaserEffectsOnlyActiveInCorrectPhase() {
        Board board = initBoardWithRobotLasers(10, 10);
        
        Robot robot = new Robot(1, 2, 2, Direction.E);
        List<Robot> robots = List.of(robot);
        
        RobotLaser laserEffect = new RobotLaser(robot);
        
        assertTrue(laserEffect.phases().contains(Phase.ACTIVATE_ROBOT_LASERS));
        assertEquals(1, laserEffect.phases().size());
        
        assertFalse(laserEffect.phases().contains(Phase.ACTIVATE_BOARD_LASERS));
        assertFalse(laserEffect.phases().contains(Phase.ACTIVATE_CHECKPOINTS));
        assertFalse(laserEffect.phases().contains(Phase.ACTIVATE_GEAR));
    }
}