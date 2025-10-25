package dk.dtu.infrastructure.web;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.BoardDto;
import dk.dtu.infrastructure.dto.RobotDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

// Author(s) Weihao Mo, William Pii Jæger

@RestController
public class GameController {
    private final GameManager gameManager;

    public GameController(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public record StartGameRequest(int amountPlayers, int boardSize) {}
    public record StartGameResponse(UUID gameID) {}
    public record EndGameRequest(String gameID) {}
    public record EndGameResponse(boolean endedGame) {}

    @PostMapping("/startGame")
    public synchronized StartGameResponse start(@RequestBody StartGameRequest req) {
        Tile[][] tiles = new Tile[req.boardSize()][req.boardSize()];
        for (int x = 0; x < req.boardSize(); x++) {
            for (int y = 0; y < req.boardSize(); y++) {
                tiles[y][x] = new Tile(y,x);
            }
        }

        Board board = new Board(req.boardSize(), req.boardSize(), tiles);
        List<Robot> robots = new ArrayList<>(5);

        Random rnd = new Random();
        for (int i = 0; i < req.amountPlayers(); i++) {
            int x = rnd.nextInt(req.boardSize());
            int y = rnd.nextInt(req.boardSize());

            int id = i + 1;

            Direction dir = Direction.values()[rnd.nextInt(Direction.values().length)];

            robots.add(new Robot(id, x, y, dir));
        }

        for (int i = 1; i <= 3; i++) {
            int x, y;
            do {
                x = rnd.nextInt(req.boardSize());
                y = rnd.nextInt(req.boardSize());
            } while (!tiles[x][y].getEffects().isEmpty());

            tiles[x][y].addEffect(new Checkpoint(i));
        }

        BoardAPI boardApi = new BoardApiImpl(board,robots);
        UUID gameID = gameManager.startGame(board, boardApi, robots);

        return new StartGameResponse(gameID);
    }

    @PostMapping("/endGame")
    public EndGameResponse start(@RequestBody EndGameRequest req) {
        gameManager.endGame(UUID.fromString(req.gameID()));
        return new EndGameResponse(true);
    }
}
