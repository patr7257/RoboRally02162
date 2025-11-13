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
import java.util.concurrent.atomic.AtomicReference;

import static dk.dtu.util.BoardTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Weihao Mo
 */
class GameCheckpointTest {

    /**
     * @author Weihao Mo
     */
    @Test
    void robotWinsAfterCompletingCheckpointsInOrder() {
        Board board = initBoardWithCheckPoints(3,3);

        Robot r= new Robot(1,0,1, Direction.E);
        r.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        AtomicReference<PlayerID> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(Game g,PlayerID winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        r.loadProgram(List.of(ProgramCard.move1()));
        game.startRound();

        assertEquals(2,r.getNextCheckpoint());
        assertTrue(game.getWinner().isEmpty());

        r.loadProgram(List.of(
                ProgramCard.right(),
                ProgramCard.move1(),
                ProgramCard.left(),
                ProgramCard.move1()
        ));
        game.startRound();
        assertEquals(2,r.getX());
        assertEquals(2,r.getY());
        assertEquals(3, r.getNextCheckpoint());
        assertEquals(new PlayerID(1), game.getWinner().orElse(null));
        assertEquals(new PlayerID(1), observedWinner.get());
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robotWinsAfterCompletingCheckpointsInWrongOrder() {
        Board board = initBoardWithCheckPointsInDifferentNumber(3,3);

        Robot r= new Robot(1,0,1, Direction.E);
        r.loadProgram(List.of(ProgramCard.move1()));
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        AtomicReference<PlayerID> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(Game g,PlayerID winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        r.loadProgram(List.of(ProgramCard.move1()));
        game.startRound();

        assertEquals(1,r.getNextCheckpoint());
        assertTrue(game.getWinner().isEmpty());

        r.loadProgram(List.of(
                ProgramCard.right(),
                ProgramCard.move1(),
                ProgramCard.left(),
                ProgramCard.move1()
        ));
        game.startRound();
        assertEquals(2,r.getX());
        assertEquals(2,r.getY());
        assertEquals(2,r.getNextCheckpoint());
        assertTrue(game.getWinner().isEmpty());
        assertNull(observedWinner.get());
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void robotsDoNotWinWhenSecondRobotSkipsCheckpoints() {
        Board board = initBoardWithCheckPointsInThreeDifferentNumber(3,3);

        Robot r1 = new Robot(1, 0, 1, Direction.E);
        Robot r2 = new Robot(2, 1, 2, Direction.E);
        List<Robot> robots = List.of(r1, r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        AtomicReference<PlayerID> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(Game g,PlayerID winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        r1.loadProgram(List.of(ProgramCard.move1()));
        r2.loadProgram(List.of(ProgramCard.move1()));
        game.startRound();

        assertEquals(1, r1.getX());
        assertEquals(1, r1.getY());
        assertEquals(2, r1.getNextCheckpoint());
        assertEquals(1, r2.getNextCheckpoint());
        assertTrue(game.getWinner().isEmpty());
        assertNull(observedWinner.get());

        r1.loadProgram(List.of(ProgramCard.right(), ProgramCard.move1()));
        r2.loadProgram(List.of());
        game.startRound();

        assertEquals(1, r1.getX());
        assertEquals(2, r1.getY());
        assertEquals(2,r2.getX());
        assertEquals(2, r2.getY());
        assertEquals(3, r1.getNextCheckpoint());
        assertEquals(1, r2.getNextCheckpoint());
        assertTrue(game.getWinner().isEmpty());
        assertNull(observedWinner.get());
    }
}
