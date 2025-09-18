package dk.dtu.infrastructure;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.infrastructure.dto.BoardDto;
import dk.dtu.infrastructure.dto.EffectDto;
import dk.dtu.infrastructure.dto.RobotDto;
import dk.dtu.infrastructure.dto.TileDto;

import java.util.ArrayList;
import java.util.List;

// Author(s) William Pii Jæger

public final class SnapshotMapper {

    public  static BoardDto toBoardDto(Board b) {
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
        return new TileDto(List.copyOf(effects));
    }

    public static List<RobotDto> mapRobots(List<Robot> robots) {
        return robots.stream().map(SnapshotMapper::mapRobot).toList();
    }

    public static RobotDto mapRobot(Robot r) {
        return new RobotDto(r.getId(), r.getX(), r.getY(), r.getDirection().name());
    }
}
