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
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[2][1].setEffects(List.of(new BoardLaser(Direction.S, 1)));
        Board board = new Board(10, 10, tiles);
        
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
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[3][1].setEffects(List.of(new BoardLaser(Direction.S, 3)));
        Board board = new Board(10, 10, tiles);
        
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
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[1][1].setEffects(List.of(new BoardLaser(Direction.S, 2)));
        tiles[1][2].setEffects(List.of(new Walls(EnumSet.of(Direction.N))));
        Board board = new Board(10, 10, tiles);
        
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
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[2][5].setEffects(List.of(new BoardLaser(Direction.E, 2)));
        Board board = new Board(10, 10, tiles);
        
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
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[2][1].setEffects(List.of(new BoardLaser(Direction.S, 1)));
        Board board = new Board(10, 10, tiles);
        
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
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[2][1].setEffects(List.of(new BoardLaser(Direction.S, 1)));
        Board board = new Board(10, 10, tiles);
        Robot robot = new Robot(1, 2, 2, Direction.E);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        DamageDecks damageDecks = new DamageDecks(38, 15, 15);
        Game game = new Game(board, api, robots);
        game.setDamageDecks(damageDecks);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        List<ProgramCard> discardAfterRound1 = game.getRobotDiscard(1);
        assertEquals(1, discardAfterRound1.size());
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        List<ProgramCard> discardAfterRound2 = game.getRobotDiscard(1);
        assertEquals(2, discardAfterRound2.size());
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        List<ProgramCard> discardAfterRound3 = game.getRobotDiscard(1);
        assertEquals(3, discardAfterRound3.size());
        
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
        tiles[5][5].setEffects(List.of(new BoardLaser(Direction.E, 2)));
        tiles[7][5].setEffects(List.of(new Antenna(Direction.N)));
        
        Board board = new Board(10, 10, tiles);
        
        Robot robot = new Robot(1, 8, 5, Direction.W);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(0, discardAfter.size());
        
        assertPosDir(robot, 8, 5, Direction.W);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void wallAndRobotSameTileWallBlocksLaserWhenFacingOpposite() {
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[3][5].setEffects(List.of(new BoardLaser(Direction.E, 1)));
        tiles[5][5].setEffects(List.of(new Walls(EnumSet.of(Direction.W))));
        
        Board board = new Board(10, 10, tiles);
        Robot robot = new Robot(1, 5, 5, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(0, discardAfter.size());
        
        assertPosDir(robot, 5, 5, Direction.N);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void wallAndRobotSameTileWallBlocksLaserWhenFacingNonOpposite() {
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[3][5].setEffects(List.of(new BoardLaser(Direction.E, 1)));
        tiles[5][5].setEffects(List.of(new Walls(EnumSet.of(Direction.N, Direction.S, Direction.E))));
        
        Board board = new Board(10, 10, tiles);
        Robot robot = new Robot(1, 5, 5, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        game.applyTileEffects(Phase.ACTIVATE_BOARD_LASERS);
        
        List<ProgramCard> discardAfter = game.getRobotDiscard(1);
        assertEquals(1, discardAfter.size());
        assertTrue(discardAfter.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        
        assertPosDir(robot, 5, 5, Direction.N);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void robotOnLaserMovesOutAndBackIsHitOnlyOnce() {
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[2][1].setEffects(List.of(new BoardLaser(Direction.S, 1)));
        Board board = new Board(10, 10, tiles);
        
        Robot robot = new Robot(1, 2, 2, Direction.W);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        List<ProgramCard> hand = new ArrayList<>();
        hand.add(ProgramCard.uturn());
        hand.add(ProgramCard.back1());
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());
        List<ProgramCard> discard = new ArrayList<>();
        
        Deck deck = new Deck(drawPile, discard, hand, new DamageDecks(38, 15, 15));
        game.setDeck(deck, 1);
        
        game.submitProgram(new PlayerID(1), List.of(
            ProgramCard.uturn(),
            ProgramCard.back1(),
            ProgramCard.move1(),
            ProgramCard.move1(),
            ProgramCard.move1()
        ), false);
        
        game.executeRegister(1);
        assertPosDir(robot, 2, 2, Direction.E);
        
        List<ProgramCard> discardAfterRegister1 = game.getRobotDiscard(1);
        assertEquals(1, discardAfterRegister1.size());
        
        game.executeRegister(2);
        assertPosDir(robot, 1, 2, Direction.E);
        
        List<ProgramCard> discardAfterRegister2 = game.getRobotDiscard(1);
        assertEquals(1, discardAfterRegister2.size());

        game.executeRegister(3);
        assertPosDir(robot, 2, 2, Direction.E);
        
        List<ProgramCard> discardAfterRegister3 = game.getRobotDiscard(1);
        assertEquals(2, discardAfterRegister3.size());
        
        long spamCount = discardAfterRegister3.stream()
                                              .filter(c -> c.action() == ProgramCard.Action.SPAM)
                                              .count();
        assertEquals(2, spamCount);
    }

    /**
     * @author Patrick Røbel
     */
    @Test
    void dontDealDamageWhenRobotMovesAwayFromLaserField() {
        Tile[][] tiles = initEmptyCells(10, 10);
        tiles[2][1].setEffects(List.of(new BoardLaser(Direction.S, 1)));
        Board board = new Board(10, 10, tiles);
        
        Robot robot = new Robot(1, 2, 2, Direction.W);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);
        
        Game game = new Game(board, api, robots);
        
        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        List<ProgramCard> hand = new ArrayList<>();
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());
        List<ProgramCard> discard = new ArrayList<>();
        
        Deck deck = new Deck(drawPile, discard, hand, new DamageDecks(38, 15, 15));
        game.setDeck(deck, 1);
        
        game.submitProgram(new PlayerID(1), List.of(
            ProgramCard.move1(),
            ProgramCard.move1(),
            ProgramCard.move1(),
            ProgramCard.move1(),
            ProgramCard.move1()
        ), false);
        
        game.executeRegister(1);
        assertPosDir(robot, 1, 2, Direction.W);
        
        List<ProgramCard> discardAfterRegister1 = game.getRobotDiscard(1);
        assertEquals(0, discardAfterRegister1.size());

        long spamCount = discardAfterRegister1.stream()
                                              .filter(c -> c.action() == ProgramCard.Action.SPAM)
                                              .count();
        assertEquals(0, spamCount);
    }
}