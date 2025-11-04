package dk.dtu;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.GameObserver;
import dk.dtu.domain.core.PlayerID;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static dk.dtu.util.GameTestSupport.*;
import static dk.dtu.util.BoardTestUtils.initBoardWithRebootToken;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Author(s) Weihao Mo

public class GameRebootTest {
    @Test
    void robotRespawn() {
        Board board = initBoardWithRebootToken(5,5);
        Robot r= new Robot(1,0,0, Direction.N);
        List<Robot> robots = List.of(r);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        AtomicReference<PlayerID> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(PlayerID winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        r.loadProgram(List.of(ProgramCard.move1()));
        game.startRound();
        assertPosDir(r,2,2,Direction.E);
    }

    @Test
    void robotRespawnAndThenPushed() {
        Board board = initBoardWithRebootToken(5,5);
        Robot r1= new Robot(1,0,0, Direction.N);
        Robot r2= new Robot(2,1,0, Direction.N);

        List<Robot> robots = List.of(r1,r2);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        AtomicReference<PlayerID> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(PlayerID winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        r1.loadProgram(List.of(ProgramCard.move1()));
        r2.loadProgram(List.of(ProgramCard.move1()));
        game.startRound();
        assertPosDir(r1,3,2,Direction.E);
        assertPosDir(r2,2,2,Direction.E);
    }


    @Test
    void threeRobotRespawnAndThenPushed() {
        Board board = initBoardWithRebootToken(5,5);
        Robot r1= new Robot(1,0,0, Direction.N);
        Robot r2= new Robot(2,1,0, Direction.N);
        Robot r3= new Robot(3,2,0, Direction.N);

        List<Robot> robots = List.of(r1,r2,r3);
        BoardAPI api = new BoardApiImpl(board, robots);
        Game game = new Game(board, api, robots);

        AtomicReference<PlayerID> observedWinner = new AtomicReference<>();
        game.addObserver(new GameObserver() {
            @Override public void onWinnerDeclared(PlayerID winner) { observedWinner.set(winner); }
            @Override public void onGameUpdate(Game g) { }
        });

        r1.loadProgram(List.of(ProgramCard.move1(), ProgramCard.move2()));
        r2.loadProgram(List.of(ProgramCard.move1(),ProgramCard.move2()));
        r3.loadProgram(List.of(ProgramCard.move1(),ProgramCard.move2()));
        game.startRound();
        assertPosDir(r1,4,2,Direction.E);
        assertPosDir(r2,3,2,Direction.E);
        assertPosDir(r3,2,2,Direction.E);
    }





}
