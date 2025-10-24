package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.core.GameObserver;
import dk.dtu.domain.core.PlayerID;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.support.NoDelayPacer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
// Author(s) Weihao Mo

class GameObserverMockTest {
    @Test
    void testWinnerNotification() {
        Board board = initEmptyBoard(5, 5);
        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 2)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);
        BoardAPI api = new BoardApiImpl(board,robots);
        Game game = new Game(board,api,robots);

        GameObserver observer = mock(GameObserver.class);
        game.addObserver(observer);
        PlayerID winner = new PlayerID(1);
        game.declareWinner(winner);

        verify(observer,times(1)).onWinnerDeclared(winner);


    }

    @Test
    void testGameManagerCanSeeWinner() {
        Board board = initEmptyBoard(5, 5);
        Robot r = new Robot(1, 1, 1, Direction.E);
        r.loadProgram(List.of(new ProgramCard(ProgramCard.Action.MOVE, 2)));
        List<Robot> robots = new ArrayList<>();
        robots.add(r);

        NoDelayPacer pacer = new NoDelayPacer();
        BoardAPI api = new BoardApiImpl(board, robots);
        GameManager manager = new GameManager(pacer);

        UUID gameId = manager.startGame(board, api, robots);
        Game game = manager.findByID(gameId).orElseThrow();

        PlayerID winner = new PlayerID(1);
        game.declareWinner(winner);

        Optional<Game> found = manager.findByID(gameId);

        assertTrue(found.isPresent());
        assertTrue(found.get().getWinner().isPresent());
       assertEquals(winner, found.get().getWinner().get());

    }
}
