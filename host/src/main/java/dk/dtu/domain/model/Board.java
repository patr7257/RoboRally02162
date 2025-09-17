package dk.dtu.domain.model;

// Author(s): William Pii Jæger

public class Board {
    private final int width;
    private final int height;
    private final Cell[][] cells;

    public Board(int width, int height, Cell[][] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Cell[][] getCells() {
        return cells;
    }
}
