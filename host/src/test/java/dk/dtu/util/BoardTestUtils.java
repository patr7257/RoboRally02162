package dk.dtu.util;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.effects.Checkpoint;

import java.util.Collections;
import java.util.List;

// Author(s) William Pii Jæger, Weihao Mo

public final class BoardTestUtils {

    private BoardTestUtils() {
    }

    public static Tile[][] initEmptyCells(int width, int height) {
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile c = new Tile(y,x);
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

    public static Board initBoardWithCheckPoints(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);

        Tile cp1 = tiles[1][1];
        cp1.setEffects(List.of(new Checkpoint(1)));

        Tile cp2 = tiles[2][2];
        cp2.setEffects(List.of(new Checkpoint(2)));

        return new Board(width,height,tiles);

    }

    public static Board initBoardWithCheckPointsInDifferentNumber(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);

        Tile cp1 = tiles[1][1];
        cp1.setEffects(List.of(new Checkpoint(2)));

        Tile cp2 = tiles[2][2];
        cp2.setEffects(List.of(new Checkpoint(1)));

        return new Board(width,height,tiles);

    }

    public static Board initBoardWithCheckPointsInThreeDifferentNumber(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);

        Tile cp1 = tiles[1][1];
        cp1.setEffects(List.of(new Checkpoint(1)));

        Tile cp2 = tiles[1][2];
        cp2.setEffects(List.of(new Checkpoint(2)));

        Tile cp3 = tiles[2][2];
        cp3.setEffects(List.of(new Checkpoint(3)));

        return new Board(width,height,tiles);

    }
}
