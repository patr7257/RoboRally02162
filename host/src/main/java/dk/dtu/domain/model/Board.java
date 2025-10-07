package dk.dtu.domain.model;

// Author(s): William Pii Jæger

import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.TileEffect;

public class Board {
    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public Board(int width, int height, Tile[][] tiles) {
        this.width = width;
        this.height = height;
        this.tiles = tiles;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile[][] getCells() {
        return tiles;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public Tile getTile(int x, int y) { return tiles[x][y]; };

}
