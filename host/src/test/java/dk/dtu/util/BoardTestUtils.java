package dk.dtu.util;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Rotation;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.effects.*;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.RebootToken;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

// Author(s) William Pii Jæger, Weihao Mo

public final class BoardTestUtils {

    private BoardTestUtils() {
    }

    public static Tile[][] initEmptyCells(int width, int height) {
        Tile[][] tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile c = new Tile(x, y);
                c.setX(x);
                c.setY(y);
                c.setEffects(Collections.emptyList());
                tiles[x][y] = c;
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

   public static Board initBoardWithBlueConveyors(int width, int height) {
       Tile[][] tiles = initEmptyCells(width,height);
       tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
       tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
       tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
       tiles[3][1].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
       tiles[3][2].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
       tiles[3][3].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
       tiles[3][4].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.LEFT)));
       tiles[4][4].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
       tiles[5][4].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
       tiles[6][4].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
       tiles[7][4].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));

       tiles[8][8].setEffects(List.of(new Checkpoint(1)));
       return new Board(width, height, tiles);
    }

    public static Board initBoardWithGreenConveyors(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[2][1].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[2][2].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[2][3].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));

        tiles[4][4].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }


    public static Board initBoardWithGreenConveyorsWithCheckpoints(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[3][1].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[3][2].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[3][3].setEffects(List.of(new Checkpoint(1)));

        return new Board(width, height, tiles);
    }


    public static Board initBoardWithBlueConveyorsWithCheckpoints(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[4][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[4][1].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[4][2].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[4][3].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[4][4].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[4][5].setEffects(List.of(new Checkpoint(1)));

        return new Board(width, height, tiles);
    }

    public static Board initBoardWithGreenConveyorCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new GreenConveyor(Direction.W, Rotation.NONE)));
        return new Board(width, height, tiles);
    }


    public static Board initBoardWithBlueConveyorCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        tiles[4][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithThreeRobotCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][1].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][1].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithSecondStepCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        tiles[4][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithBlueConveyorCancelMove(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithGreenConveyorAndWalls(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[1][1].setEffects(List.of(new Walls(EnumSet.of(Direction.N))));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width,height,tiles);
    }


    public static Board initBoardWithBlueConveyorAndWalls(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[1][1].setEffects(List.of(new Walls(EnumSet.of(Direction.N))));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width,height,tiles);
    }


    public static Board initBoardWithCurvedConveyorAtDestination(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }


    public static Board initBoardWithCurvedConveyorAtDestinationBlue(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[1][2].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }


    public static Board initBoardWithStraightThenCurvedGreenConveyor(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithStraightThenCurvedBlueConveyor(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithStraightBlue(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }


    public static Board initBoardWithStraightGreen(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithCurvedToCurvedGreenConveyor(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new GreenConveyor(Direction.W, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithMultipleCurvedBlueConveyors(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    public static Board initBoardWithRebootToken(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);
        Tile cp1 = tiles[2][2];
        cp1.setEffects(List.of(new RebootToken(Direction.E)));

        Tile cp2 = tiles[4][4];
        cp2.setEffects(List.of(new Checkpoint(1)));

        return new Board(width,height,tiles);
    }
}
