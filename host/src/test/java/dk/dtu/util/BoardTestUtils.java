package dk.dtu.util;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;

import java.util.Collections;

// Author(s) William Pii Jæger

public final class BoardTestUtils {

    private BoardTestUtils() {
    }

    public static Tile[][] initEmptyCells(int width, int height) {
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile c = new Tile();
                c.setX(x);
                c.setY(y);
                c.setEffects(Collections.emptyList());
                tiles[y][x] = c;
            }
        }
        return tiles;
    }

    public static Board initEmptyBoard(int width, int height) {
        return new Board(width, height, initEmptyCells(width, height));
    }
}
