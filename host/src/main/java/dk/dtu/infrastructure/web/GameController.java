package dk.dtu.infrastructure.web;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.RebootToken;
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

    private ArrayList<Tile> randomTilesWithoutFacingWallsAndEdges(Board board) {
        Random rnd = new Random();
        ArrayList<Tile> selectedTiles = new ArrayList<>();

        int maxAttempts = board.getWidth() * board.getHeight() * 10;
        int attempts = 0;

        while (selectedTiles.isEmpty() && attempts < maxAttempts) {
            attempts++;

            int x = rnd.nextInt(board.getWidth());
            int y = rnd.nextInt(board.getHeight());

            Tile tile = board.getTile(x, y);

            if (!tile.getEffects().isEmpty()) {
                continue;
            }

            Direction[] directions = Direction.values();
            List<Direction> validDirections = new ArrayList<>();

            for (Direction dir : directions) {
                if (isValidRebootDirection(board, tile, dir)) {
                    validDirections.add(dir);
                }
            }

            if (!validDirections.isEmpty()) {
                selectedTiles.add(tile);
            }
        }

        return selectedTiles;
    }

    private boolean isValidRebootDirection(Board board, Tile tile, Direction dir) {
        int x = tile.getX();
        int y = tile.getY();

        int nextX = x;
        int nextY = y;

        for (int step = 1; step <= 6; step++) {
            switch (dir) {
                case N -> nextY = y - step;
                case S -> nextY = y + step;
                case E -> nextX = x + step;
                case W -> nextX = x - step;
            }

            if (!board.isInBounds(nextX, nextY)) {
                return false;
            }

            Tile currentTile = (step == 1) ? tile : board.getTile(
                    dir == Direction.E || dir == Direction.W ? x + (step - 1) * (dir == Direction.E ? 1 : -1) : x,
                    dir == Direction.N || dir == Direction.S ? y + (step - 1) * (dir == Direction.S ? 1 : -1) : y
            );

            Tile nextTile = board.getTile(nextX, nextY);

            if (Walls.hasWall(currentTile, dir)) {
                return false;
            }
            if (Walls.hasWall(nextTile, dir.opposite())) {
                return false;
            }
        }

        return true;
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


        ArrayList<Tile> rebootTiles = randomTilesWithoutFacingWallsAndEdges(board);

        if (!rebootTiles.isEmpty()) {
            Tile rebootTile = rebootTiles.get(0);

            Direction[] directions = Direction.values();
            List<Direction> validDirections = new ArrayList<>();

            for (Direction dir : directions) {
                if (isValidRebootDirection(board, rebootTile, dir)) {
                    validDirections.add(dir);
                }
            }

            if (!validDirections.isEmpty()) {
                Direction rebootDir = validDirections.get(rnd.nextInt(validDirections.size()));
                rebootTile.addEffect(new RebootToken(rebootDir));
            }
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
