package dk.dtu;

import dk.dtu.domain.core.*;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.support.NoDelayPacer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;
import static org.junit.jupiter.api.Assertions.*;

// Author(s) Weihao Mo

class GameManagerSubmitProgramsTest {

    @Test
    void testSubmitProgramsLoadsRegisters() {
        Board board = initEmptyBoard(3, 3);
        Robot r = new Robot(1, 1, 1, Direction.E);
        List<Robot> robots = new ArrayList<>(List.of(r));
        BoardAPI api = new BoardApiImpl(board, robots);

        NoDelayPacer pacer = new NoDelayPacer();
        GameManager manager = new GameManager(pacer);
        UUID gid = manager.startGame(board, api, robots);

        var start = new GameCommand.StartProgramming(UUID.randomUUID(), gid, 60_000);
        assertTrue(manager.execute(start).ok());

        var submit = new GameCommand.SubmitPrograms(UUID.randomUUID(), gid, new PlayerID(1),
                List.of(ProgramCard.move1()));
        assertTrue(manager.execute(submit).ok());

        ProgramOP op = r.getRegisters().getFirst();
        assertTrue(op instanceof ProgramOP.Move);
        assertEquals(1, ((ProgramOP.Move) op).steps());
    }
}
