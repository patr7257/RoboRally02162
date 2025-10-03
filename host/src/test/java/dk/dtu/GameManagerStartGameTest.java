package dk.dtu;

import dk.dtu.domain.core.CommandResult;
import dk.dtu.domain.core.GameCommand;
import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dk.dtu.util.BoardTestUtils.initEmptyBoard;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Author(s) Weihao Mo

class GameManagerStartGameTest {

    @Test
    void testStartRoundCommand() {
        Board board = initEmptyBoard(3, 3);
        List<Robot> robots = new ArrayList<>();

        Robot r = new Robot(1, 1, 1, Direction.E);
        robots.add(r);
        r.loadProgram(List.of(ProgramCard.move1()));
        BoardAPI api = new BoardApiImpl(board,robots);

        GameManager manager = new GameManager();
        UUID gid = manager.startGame(board, api, List.of(r));

        CommandResult result = manager.apply(gid, new GameCommand.StartRound());

        assertEquals("OK", result.reason());
    }
}
