package dk.dtu.infrastructure;

import dk.dtu.domain.core.Game;
import dk.dtu.domain.core.PlayerID;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.RebootToken;
import dk.dtu.domain.rules.effects.StartingTile;
import dk.dtu.domain.rules.effects.Walls;
import dk.dtu.infrastructure.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Author(s) William Pii Jæger, Weihao Mo

public final class SnapshotMapper {

    public static SnapshotPayload createSnapshot(UUID gameId, Game game) {
        GameDto gameDto = mapGame(gameId, game);
        BoardDto board = toBoardDto(game.getBoard());
        List<RobotDto> robots = mapRobots(game.getRobots());
        return new SnapshotPayload(gameDto, board, robots);
    }

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
                if (effect instanceof StartingTile st) {
                    effects.add(new StartingTileDto(st.playerId()));
                }
                if (effect instanceof RebootToken rt) {
                    effects.add(new RebootTokenDto(rt.direction()));
                }
            }
        }

        return new TileDto(List.copyOf(effects));
    }

    public static List<RobotDto> mapRobots(List<Robot> robots) {
        return robots.stream().map(SnapshotMapper::mapRobot).toList();
    }

    public static RobotDto mapRobot(Robot r) {
        return new RobotDto(r.getId(), r.getX(), r.getY(), r.getDirection().name(), r.getNextCheckpoint());
    }

    public static GameDto mapGame(UUID gameID, Game game) {
        Integer winner = game.getWinner().map(PlayerID::value).orElse(null);
        return new GameDto(gameID, winner);
    }
}
