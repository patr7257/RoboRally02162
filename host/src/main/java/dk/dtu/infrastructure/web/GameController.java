package dk.dtu.infrastructure.web;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.core.GameQuery;
import dk.dtu.domain.core.GameSession;
import dk.dtu.domain.core.GameState;
import dk.dtu.domain.model.*;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.model.*;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.TileEffect;
import dk.dtu.domain.rules.effects.RebootToken;
import dk.dtu.domain.rules.effects.Gear;
import dk.dtu.domain.rules.effects.Walls;
import dk.dtu.domain.rules.effects.StartingTile;
import dk.dtu.domain.rules.effects.*;
import dk.dtu.infrastructure.SnapshotMapper;
import dk.dtu.infrastructure.dto.BoardDto;
import dk.dtu.infrastructure.dto.RobotDto;
import dk.dtu.infrastructure.dto.SnapshotPayload;
import dk.dtu.infrastructure.utils.ConveyorBeltPatterns;
import dk.dtu.infrastructure.utils.BoardTemplateConverter;
import dk.dtu.infrastructure.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller responsible for managing game operations including starting, ending, saving, and loading games
 *
 * @author Weihao Mo
 * @author William Pii Jæger
 * @author Bjarke Søderhamn Petersen
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 */
@RestController
public class GameController {
    private final GameManager gameManager;

    public GameController(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    public record StartGameRequest(int amountPlayers, int boardSize) {}
    public record StartGameWithTemplateRequest(int amountPlayers, JsonNode boardTemplate) {}
    public record StartGameResponse(UUID gameID) {}
    public record EndGameRequest(String gameID) {}
    public record EndGameResponse(boolean endedGame) {}

    public record SaveGameRequest(String gameID) {}
    public record SaveGameResponse(Object snapshotPayload, Object decks){}

    public record SnapshotLoadedPayload(GameDto game, BoardDto board, List<RobotDto> robots){}
    public record DeckDto(List<ProgramCard> drawPile, List<ProgramCard> discardPile, List<ProgramCard> hand){}
    public record GameInfo(SnapshotLoadedPayload snapshotPayload, Map<Integer, DeckDto> decks){}
    public record StartLoadedGameRequest(int amountPlayers, int boardSize, String gameID, GameInfo gameInfo) {}

    /**
     * Selects random empty tiles from the board within a specified range.
     * Only tiles without any effects are considered for selection.
     *
     * @param floor the smallest number of tiles
     * @param ceil the biggest number of tiles
     * @param board the game board
     * @return a list of randomly selected empty tiles
     * @throws IllegalStateException if there are not enough empty tiles available
     * @author William Pii Jæger
     */
    private ArrayList<Tile> randomTiles(int floor, int ceil, Board board) {
        int toPick = Math.max(0, ceil - floor + 1);
        ArrayList<Tile> pool = new ArrayList<>();

        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Tile t = board.getTile(x, y);
                if (t.getEffects().isEmpty()) pool.add(t);
            }
        }

        if (pool.size() < toPick) {
            throw new IllegalStateException("Not enough empty tiles to place " + toPick + " items");
        }

        Collections.shuffle(pool, new Random());
        return new ArrayList<>(pool.subList(0, toPick));
    }

    /**
     * Finds random empty tiles that can place reboot token.
     * A tile is selected if it has at least one direction where a robot can move 6 spaces
     * without hitting walls or going out of bounds.
     *
     * @param board the game board to select tiles from
     * @return a list containing one suitable tile if found, or an empty list if none found
     * @author Weihao Mo
     */
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

    /**
     * Validates whether a reboot token can be placed on a tile facing a specific direction.
     * Checks if a robot can move 6 spaces in this direction without walls or pits or going out of bounds.
     *
     * @param board the game board
     * @param tile the tile to check from
     * @param dir the direction to check if it fits the condition
     * @return true if the direction is valid for reboot token, false otherwise
     * @author Weihao Mo
     */
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

            if (Walls.hasWall(currentTile, dir) || Pits.hasPits(currentTile)) {
                return false;
            }
            if (Walls.hasWall(nextTile, dir.opposite()) || Pits.hasPits(nextTile)) {
                return false;
            }
        }

        return true;
    }


    /**
     * Handles the request to start a new game
     *
     * @param req the request containing the number of players and board size
     * @return a response containing the unique game ID of the newly created game
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    @PostMapping("/startGame")
    public synchronized StartGameResponse start(@RequestBody StartGameRequest req) {
        int width = req.boardSize();
        int totalHeight = req.boardSize() + 2;

        Tile[][] tiles = new Tile[width][totalHeight];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < totalHeight; y++) {
                tiles[x][y] = new Tile(x,y);
            }
        }

        Board board = new Board(width, totalHeight, tiles);
        List<Robot> robots = new ArrayList<>(5);

        Random rnd = new Random();

        if(rnd.nextBoolean()) {
            ConveyorBeltPatterns.applyHighOctane(board);
        } else {
            ConveyorBeltPatterns.applyCorridorBlitz(board);
        }

        // Place robots in starting area (rows 0-1) with starting tiles
        int[][] startingPositions = {
            {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0}, {8, 0},
            {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {7, 1}, {8, 1}
        };

        for (int i = 0; i < req.amountPlayers() && i < startingPositions.length; i++) {
            int x = startingPositions[i][0];
            int y = startingPositions[i][1];
            int id = i + 1;

            Direction dir = Direction.S;

            robots.add(new Robot(id, x, y, dir));

            // Add starting tile effect
            board.getTile(x, y).addEffect(new StartingTile(id));

            board.getTile(board.getWidth()/2, 0).addEffect(new Antenna(Direction.S));
            board.getTile(board.getWidth()/2, 0).addEffect(new Walls(EnumSet.of(Direction.W,Direction.N,Direction.E,Direction.S)));
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

        ArrayList<Tile> gearTiles = randomTiles(0, 5, board);
        Rotation[] gearDirs = { Rotation.RIGHT, Rotation.LEFT };

        for (Tile t : gearTiles) {
            t.addEffect(new Gear(gearDirs[rnd.nextInt(gearDirs.length)]));
        }

        ArrayList<Tile> pitsTiles = randomTiles(0,3,board);
        for (Tile t: pitsTiles) {
            t.addEffect(new Pits());
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

    /**
     * @author Patrick Røbel
     */
    @PostMapping("/startGameWithTemplate")
    public synchronized StartGameResponse startWithTemplate(@RequestBody StartGameWithTemplateRequest req) {
        // Convert JSON template to Board using helper class
        Board board = BoardTemplateConverter.convertTemplateToBoard(req.boardTemplate());
        
        // Create robots from the template's starting tiles
        List<Robot> robots = BoardTemplateConverter.createRobotsFromTemplate(board, req.amountPlayers());
        
        // Create the board API and start the game (same as existing logic)
        BoardAPI boardApi = new BoardApiImpl(board, robots);
        UUID gameID = gameManager.startGame(board, boardApi, robots);

        return new StartGameResponse(gameID);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @PostMapping("/startLoadedGame")
    public synchronized StartGameResponse start(@RequestBody StartLoadedGameRequest req) {
        SnapshotLoadedPayload snapshot = req.gameInfo().snapshotPayload();
        BoardDto boardDto = snapshot.board();

        Board board = SnapshotMapper.fromBoardDto(boardDto);
        List<Robot> robots = SnapshotMapper.fromRobotDtos(snapshot.robots());
        Map<Integer, Deck> deckMap = SnapshotMapper.fromMapDeckDto(req.gameInfo().decks());

        BoardAPI boardApi = new BoardApiImpl(board, robots);

        UUID gameID = gameManager.startGame(board, boardApi, robots, deckMap);

        return new StartGameResponse(gameID);
    }

    /**
     * Handles the request to end a game
     *
     * @param req the request containing the game ID to end
     * @return a response indicating the game is ended
     * @author Weihao Mo
     * @author William Pii Jæger
     */
    @PostMapping("/endGame")
    public EndGameResponse start(@RequestBody EndGameRequest req) {
        gameManager.endGame(UUID.fromString(req.gameID()));
        return new EndGameResponse(true);
    }


    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    @PostMapping("/saveGame")
    public SaveGameResponse save(@RequestBody SaveGameRequest req) throws Exception {
        UUID gameID = UUID.fromString(req.gameID());

        GameSession session = gameManager.getActiveSessions().get(gameID);

        if (session.getState() == GameState.EXECUTING) {
            throw new Exception("Cannot save while executing!");
        }

        List<Integer> players = session.getGame().getRobots().stream().map(Robot::getId).toList();

        Optional<SnapshotPayload> snapOpt = gameManager.query(gameID, new GameQuery.GetSnapshot());
        SnapshotPayload snapShotPayload = snapOpt.get();

        Map<Integer, Map<String, Object>> decks = new HashMap<>();

        for (Integer pid : players) {
            Deck deck = session.getGame().getDeckMap().get(pid);
            decks.put(
                pid,
                    Map.of(
                       "drawPile", deck.getDrawPile(),
                       "discardPile", deck.getDiscardPile(),
                       "hand", deck.getHand()
                    )
            );
        }

        return new SaveGameResponse(snapShotPayload, decks);
    }
}