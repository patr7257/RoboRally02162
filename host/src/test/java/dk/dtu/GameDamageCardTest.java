package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.model.*;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.*;

import static dk.dtu.util.BoardTestUtils.*;
import static dk.dtu.util.GameTestSupport.assertPosDir;
import static org.junit.jupiter.api.Assertions.*;
/**
 * @author Weihao Mo
 */
public class GameDamageCardTest {
    /**
     * @author Weihao Mo
     */
    @Test
    void damageCardsStayInHand() {
        Board board = initEmptyBoard(3, 3);
        Robot r = new Robot(1, 1, 1, Direction.E);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        for(int i = 0; i < 7; i++) drawPile.add(ProgramCard.move1());

        List<ProgramCard> hand = new ArrayList<>();
        hand.add(ProgramCard.spam());
        hand.add(ProgramCard.trojanHorse());
        hand.add(ProgramCard.worm());
        hand.add(ProgramCard.move1());
        hand.add(ProgramCard.move1());

        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand, new DamageDecks(38,15,15));
        Game game = new Game(board, api, robots);
        game.setDeck(deck,1);

        game.dealNewHands();

        List<ProgramCard> newHand = game.getRobotHand(1);

        assertTrue(newHand.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
        assertTrue(newHand.stream().anyMatch(c -> c.action() == ProgramCard.Action.TROJAN_HORSE));
        assertTrue(newHand.stream().anyMatch(c -> c.action() == ProgramCard.Action.WORM));

        assertEquals(9, newHand.size());
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void spamCardPlaysTopCard() {
        Board board = initEmptyBoard(5, 5);
        Robot r = new Robot(1, 1, 1, Direction.E);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        drawPile.push(ProgramCard.move1());

        List<ProgramCard> hand = new ArrayList<>();
        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand,new DamageDecks(38,15,15));
        Game game = new Game(board, api, robots);
        game.setDeck(deck,1);


        r.loadProgram(List.of(ProgramCard.spam()));

        game.executeOneRobotTurn(r);

        assertEquals(2, r.getX());
        assertEquals(1, r.getY());

        List<ProgramCard> discardPile = game.getRobotDiscard(1);
        assertTrue(discardPile.stream().anyMatch(c -> c.action() == ProgramCard.Action.MOVE));
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void landsOnPitsReboot() {
        Board board = initBoardWithRebootTokenAndPits(5,5);
        Robot r = new Robot(1, 0, 0, Direction.S);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();

        List<ProgramCard> hand = new ArrayList<>();
        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand,new DamageDecks(38,15,15));
        Game game = new Game(board, api, robots);
        game.setDeck(deck,1);


        r.loadProgram(List.of(ProgramCard.move1()));

        game.executeRegister(1);

        assertPosDir(r, 0, 1, Direction.S);
        assertFalse(r.isAlive());

        List<ProgramCard> discardPile = game.getRobotDiscard(1);
        assertTrue(discardPile.stream().anyMatch(c -> c.action() == ProgramCard.Action.SPAM));
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void landsOnPitsRebootWithTrojan() {
        Board board = initBoardWithRebootTokenAndPits(5,5);
        Robot r = new Robot(1, 0, 0, Direction.S);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        DamageDecks damageDecks = new DamageDecks(0, 15, 15);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();

        List<ProgramCard> hand = new ArrayList<>();
        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand,new DamageDecks(38,15,15));
        Game game = new Game(board, api, robots);
        game.setDeck(deck,1);
        game.setDamageDecks(damageDecks);


        r.loadProgram(List.of(ProgramCard.move1()));

        game.executeRegister(1);

        assertPosDir(r, 0, 1, Direction.S);
        assertFalse(r.isAlive());

        List<ProgramCard> discardPile = game.getRobotDiscard(1);
        assertTrue(discardPile.stream().anyMatch(c -> c.action() == ProgramCard.Action.TROJAN_HORSE || c.action() == ProgramCard.Action.WORM));
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void trojanHorse_withEnoughSpam_adds2SpamToDiscard() {
        Board board = initEmptyBoard(10,10);
        Robot robot = new Robot(1, 0, 0, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);

        DamageDecks damageDecks = new DamageDecks(5, 15, 15);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        drawPile.add(ProgramCard.left());

        List<ProgramCard> hand = new ArrayList<>();
        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand,new DamageDecks(38,15,15));

        Game game = new Game(board, api, robots);
        game.setDeck(deck, 1);
        game.setDamageDecks(damageDecks);

        robot.loadProgram(List.of(ProgramCard.trojanHorse()));

        game.executeOneRobotTurn(robot);

        List<ProgramCard> discardPile = game.getRobotDiscard(1);
        long spamCount = discardPile.stream()
                .filter(c -> c.action() == ProgramCard.Action.SPAM)
                .count();

        assertEquals(2, spamCount);

        assertEquals(3, game.getDamageDecks().getSpamDrawPile());

        assertEquals(16, game.getDamageDecks().getTrojanHorseDrawPile());
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void trojanHorse_withInsufficientSpam_addsRandomDamageCards() {
        Board board = initEmptyBoard(10, 10);
        Robot robot = new Robot(1, 0, 0, Direction.N);
        List<Robot> robots = List.of(robot);
        BoardAPI api = new BoardApiImpl(board, robots);

        DamageDecks damageDecks = new DamageDecks(0, 15, 15);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        drawPile.add(ProgramCard.left());

        List<ProgramCard> hand = new ArrayList<>();
        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand, new DamageDecks(38, 15, 15));

        Game game = new Game(board, api, robots);
        game.setDeck(deck, 1);
        game.setDamageDecks(damageDecks);

        robot.loadProgram(List.of(ProgramCard.trojanHorse()));

        game.executeOneRobotTurn(robot);

        List<ProgramCard> discardPile = game.getRobotDiscard(1);
        long spamCount = discardPile.stream()
                .filter(c -> c.action() == ProgramCard.Action.SPAM)
                .count();
        assertEquals(0, spamCount);

        long trojanInDiscard = discardPile.stream()
                .filter(c -> c.action() == ProgramCard.Action.TROJAN_HORSE)
                .count();
        long wormInDiscard = discardPile.stream()
                .filter(c -> c.action() == ProgramCard.Action.WORM)
                .count();

        assertEquals(2, trojanInDiscard + wormInDiscard);

        assertEquals(0, game.getDamageDecks().getSpamDrawPile());

        int totalDamageAfter = game.getDamageDecks().getTrojanHorseDrawPile() +
                game.getDamageDecks().getWormDrawPile();
        assertEquals(29, totalDamageAfter);

        int trojanPile = game.getDamageDecks().getTrojanHorseDrawPile();
        assertTrue(trojanPile >= 14 && trojanPile <= 16);
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void spamCardExecutedThenRobotDies_spamRemovedAndPenaltyApplied() {
        Board board = initEmptyBoard(3, 3);
        Robot r = new Robot(1, 2, 1, Direction.E);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);

        Deque<ProgramCard> drawPile = new ArrayDeque<>();
        drawPile.add(ProgramCard.move1());
        List<ProgramCard> hand = new ArrayList<>();
        hand.add(ProgramCard.spam());
        hand.add(ProgramCard.left());
        hand.add(ProgramCard.right());
        hand.add(ProgramCard.move2());
        hand.add(ProgramCard.uturn());

        List<ProgramCard> discard = new ArrayList<>();

        Deck deck = new Deck(drawPile, discard, hand,new DamageDecks(38,15,15));
        Game game = new Game(board, api, robots);
        game.setDeck(deck, 1);

        r.loadProgram(List.of(
                ProgramCard.spam(),
                ProgramCard.left(),
                ProgramCard.right(),
                ProgramCard.move2(),
                ProgramCard.uturn()
        ));

        game.executeOneRobotTurn(r);

        assertFalse(r.isAlive());

        assertEquals(3, r.getX());
        assertEquals(1, r.getY());

        List<ProgramCard> discardPile = game.getRobotDiscard(1);
        assertTrue(discardPile.stream().anyMatch(c -> c.action() == ProgramCard.Action.MOVE));

        long damageCardsInDiscard = discardPile.stream()
                .filter(c -> c.action() == ProgramCard.Action.SPAM)
                .count();
        assertEquals(2, damageCardsInDiscard);

        assertEquals(37, game.getDamageDecks().getSpamDrawPile());
    }


}
