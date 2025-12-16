package dk.dtu.infrastructure;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.PlayerID;
import dk.dtu.domain.model.*;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.RebootToken;
import dk.dtu.domain.rules.effects.StartingTile;
import dk.dtu.domain.rules.effects.Gear;
import dk.dtu.domain.rules.effects.Walls;
import dk.dtu.domain.rules.effects.*;
import dk.dtu.infrastructure.dto.*;
import dk.dtu.infrastructure.web.GameController;

import java.util.*;

/**
 * A class for mapping between domain objects and data transfer objects (DTOs).
 * @author William Pii Jæger
 * @author Weihao Mo
 * @author Karl Johannes Agerbo
 */
public final class SnapshotMapper {

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public static SnapshotPayload createSnapshot(Game game) {
        BoardDto board = toBoardDto(game.getBoard());
        List<RobotDto> robots = mapRobots(game.getRobots());
        return new SnapshotPayload(board, robots);
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public static BoardDto toBoardDto(Board b) {
        int w = b.getWidth(), h = b.getHeight();
        TileDto[][] tiles = new TileDto[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                tiles[x][y] = mapTile(b.getTile(x, y));
            }
        }

        return new BoardDto(w, h, tiles);
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    private static TileDto mapTile(Tile t) {
        List<EffectDto> effects = new ArrayList<>();
        // t is null? Game has a check for null
        // We should instantiate all tiles!
        // uncommenting it for now
        //for (var effect: t.getEffects()) {
        //    // We will add effects here like laser, gear ect.
        //}
        if (t != null && t.getEffects() != null) {
            //        System.out.println("Tile effects at snapshot: " + t.getEffects());
            for (var effect : t.getEffects()) {
                if (effect instanceof Checkpoint cp) {
                    effects.add(new CheckpointDto(cp.number()));
                }
                if (effect instanceof Walls wl) {
                    effects.add(new WallDto(List.copyOf(wl.getEdges())));
                }
                if (effect instanceof BlueConveyor bc) {
                    effects.add(new BlueConveyorDto(bc.direction(), bc.rotation()));
                }
                if (effect instanceof GreenConveyor gc) {
                    effects.add(new GreenConveyorDto(gc.direction(), gc.rotation()));
                }
                if (effect instanceof StartingTile st) {
                    effects.add(new StartingTileDto(st.playerId()));
                }
                if (effect instanceof RebootToken rt) {
                    effects.add(new RebootTokenDto(rt.direction()));
                }
                if (effect instanceof Antenna at) {
                    effects.add(new AntennaDto(at.direction()));
                }
                if (effect instanceof Gear gr) {
                    effects.add(new GearDto(gr.rotation()));
                }
                if (effect instanceof Pits p) {
                    effects.add(new PitsDto());
                }
                if (effect instanceof BoardLaser bl) {
                    effects.add(new BoardLaserDto(bl.direction(), bl.power()));
                }
                if (effect instanceof RobotLaser rl) {
                    effects.add(new RobotLaserDto(rl.robot().getDirection(), rl.robot().getId()));
                }
            }
        }

        return new TileDto(List.copyOf(effects));
    }

    /**
     @author Karl Johannes Agerbo
     @author Patrick Røbel
     */
    public static Board fromBoardDto(BoardDto dto) {
        int w = dto.width();
        int h = dto.height();
        Tile[][] tiles = new Tile[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                tiles[x][y] = fromTileDto(dto.tiles()[x][y], x, y);
            }
        }

        Board board = new Board(w, h, tiles);

        return board;
    }

    /**
     @author Karl Johannes Agerbo
     */
    //TODO: Should be updated after each new tile effect is implemented.
    public static Tile fromTileDto(TileDto dto, int x, int y) {
        Tile tile = new Tile(x, y);
        for (EffectDto effect : dto.effects()) {
            if (effect instanceof CheckpointDto(int number)) {
                tile.addEffect(new Checkpoint(number));
            }
            if (effect instanceof WallDto(List<Direction> walls)) {
                tile.addEffect(new Walls(EnumSet.copyOf(walls)));
            }
            if (effect instanceof StartingTileDto(int playerId)) {
                tile.addEffect(new StartingTile(playerId));
            }
            if (effect instanceof RebootTokenDto(Direction direction)) {
                tile.addEffect(new RebootToken(direction));
            }
            if (effect instanceof AntennaDto(Direction direction)) {
                tile.addEffect(new Antenna(direction));
            }
            if (effect instanceof BlueConveyorDto(Direction direction, Rotation rotation) ) {
                tile.addEffect(new BlueConveyor(direction, rotation));
            }
            if (effect instanceof GreenConveyorDto(Direction direction, Rotation rotation) ) {
                tile.addEffect(new GreenConveyor(direction, rotation));
            }
            if (effect instanceof GearDto(Rotation rotation)) {
                tile.addEffect(new Gear(rotation));
            }
            if (effect instanceof PitsDto()) {
                tile.addEffect(new Pits());
            }
            if (effect instanceof BoardLaserDto(Direction direction, int power)) {
                tile.addEffect(new BoardLaser(direction, power));
            }
            /*
            if (effect instanceof RobotLaserDto(Direction direction, int robotId)) {
                // Robot reference is not available here; it will be set when mapping robots
                tile.addEffect(new RobotLaser(null));
            } */
        }
        return tile;
    }

    /**
     * @author William Pii Jæger
     */
    public static List<RobotDto> mapRobots(List<Robot> robots) {
        return robots.stream().map(SnapshotMapper::mapRobot).toList();
    }

    /**
     * @author William Pii Jæger
     */
    public static RobotDto mapRobot(Robot r) {
        return new RobotDto(r.getId(), r.getX(), r.getY(), r.getDirection().name(), r.getNextCheckpoint());
    }

    /**
     @author Karl Johannes Agerbo
     */
    public static List<Robot> fromRobotDtos(List<RobotDto> dtos) {
        return dtos.stream().map(SnapshotMapper::fromRobotDto).toList();
    }

    /**
     @author Karl Johannes Agerbo
     */
    public static Robot fromRobotDto(RobotDto dto) {
        return new Robot(dto.id(), dto.x(), dto.y(), Direction.valueOf(dto.facing()), dto.nextCheckpoint());
    }

    /**
     * @author Weihao Mo
     */
    public static GameDto mapGame(UUID gameID, Game game) {
        Integer winner = game.getWinner().map(PlayerID::value).orElse(null);
        return new GameDto(gameID, winner);
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Weihao Mo
     */
    public static Map<Integer, Deck> fromMapDeckDto(Map<Integer, GameController.DeckDto> deckDtoMap, DamageDecks sharedDamageDecks) {
        if (sharedDamageDecks == null) {
            sharedDamageDecks = new DamageDecks(38, 15, 15);
        }

        Map<Integer, Deck> deckMap = new HashMap<>();
        for (Map.Entry<Integer, GameController.DeckDto> entry : deckDtoMap.entrySet()) {
            Integer playerId = entry.getKey();
            GameController.DeckDto dto = entry.getValue();

            Deck deck = new Deck(
                    new ArrayDeque<>(dto.drawPile()),
                    dto.discardPile(),
                    dto.hand(),
                    sharedDamageDecks
            );
            deckMap.put(playerId, deck);
        }
        return deckMap;
    }
}