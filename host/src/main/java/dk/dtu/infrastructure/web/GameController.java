package dk.dtu.infrastructure.web;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.Walls;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.BoardDto;
import dk.dtu.infrastructure.dto.RobotDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

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

    private ArrayList<Tile> randomTiles(int floor, int ceil, Board board) {
        Random rnd = new Random();
        ArrayList<Tile> selectedTiles = new ArrayList<>();
        for (int i = floor; i <= ceil; i++) {
            int x, y;
            do {
                x = rnd.nextInt(board.getWidth());
                y = rnd.nextInt(board.getHeight());
            } while (!board.getTile(x, y).getEffects().isEmpty());

            selectedTiles.add(board.getTile(x, y));
        }

        return selectedTiles;
    }


    @PostMapping("/startGame")
    public synchronized StartGameResponse start(@RequestBody StartGameRequest req) {
        Tile[][] tiles = new Tile[req.boardSize()][req.boardSize()];
        for (int x = 0; x < req.boardSize(); x++) {
            for (int y = 0; y < req.boardSize(); y++) {
                tiles[x][y] = new Tile(x,y);
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

        ArrayList<Tile> checkPointTiles = randomTiles(1, 3, board);
        for (int i = 0; i <= 2; i++) {
            checkPointTiles.get(i).addEffect(new Checkpoint(i+1));
        }

        ArrayList<Tile> wallTiles = randomTiles(0, 5, board);
        Direction[] dirs = Direction.values();

        for (Tile t : wallTiles) {
            Direction d = dirs[rnd.nextInt(dirs.length)];
            t.addEffect(new Walls(EnumSet.of(d)));
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
