package dk.dtu.util;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Cell;

import java.util.Collections;

// Author(s) William Pii Jæger

public final class BoardTestUtils {

    private BoardTestUtils() {
    }

    public static Cell[][] initEmptyCells(int width, int height) {
        Cell[][] cells = new Cell[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell c = new Cell();
                c.setX(x);
                c.setY(y);
                c.setEffects(Collections.emptyList());
                cells[y][x] = c;
            }
        }
        return cells;
    }

    public static Board initEmptyBoard(int width, int height) {
        return new Board(width, height, initEmptyCells(width, height));
    }
}
