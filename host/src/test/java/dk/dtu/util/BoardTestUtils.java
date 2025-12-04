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

/**
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public final class BoardTestUtils {

    private BoardTestUtils() {
    }

    /**
     * @author William Pii Jæger
     */
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


    /**
     * @author William Pii Jæger
     */
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

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithCheckPointsInDifferentNumber(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);

        Tile cp1 = tiles[1][1];
        cp1.setEffects(List.of(new Checkpoint(2)));

        Tile cp2 = tiles[2][2];
        cp2.setEffects(List.of(new Checkpoint(1)));

        return new Board(width,height,tiles);

    }

    /**
     * @author Weihao Mo
     */
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

    /**
     * @author Weihao Mo
     */
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

    /**
     * @author Weihao Mo
     */
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

    /**
     * @author Weihao Mo
     */
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

    /**
     * @author Weihao Mo
     */
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

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithGreenConveyorCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new GreenConveyor(Direction.W, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithBlueConveyorCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        tiles[4][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithThreeRobotCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][1].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][1].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithSecondStepCollision(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        tiles[4][0].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithBlueConveyorCancelMove(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithGreenConveyorAndWalls(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[1][1].setEffects(List.of(new Walls(EnumSet.of(Direction.N))));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width,height,tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithBlueConveyorAndWalls(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[1][1].setEffects(List.of(new Walls(EnumSet.of(Direction.N))));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width,height,tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithCurvedConveyorAtDestination(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithCurvedConveyorAtDestinationBlue(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[1][2].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithStraightThenCurvedGreenConveyor(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithStraightThenCurvedBlueConveyor(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithStraightBlue(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithStraightGreen(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[2][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[3][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithCurvedToCurvedGreenConveyor(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new GreenConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new GreenConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new GreenConveyor(Direction.W, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithMultipleCurvedBlueConveyors(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        tiles[0][0].setEffects(List.of(new BlueConveyor(Direction.E, Rotation.NONE)));
        tiles[1][0].setEffects(List.of(new BlueConveyor(Direction.S, Rotation.RIGHT)));
        tiles[1][1].setEffects(List.of(new BlueConveyor(Direction.W, Rotation.RIGHT)));
        tiles[6][6].setEffects(List.of(new Checkpoint(1)));
        return new Board(width, height, tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithRebootToken(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);
        Tile cp1 = tiles[2][2];
        cp1.setEffects(List.of(new RebootToken(Direction.E)));

        Tile cp2 = tiles[4][4];
        cp2.setEffects(List.of(new Checkpoint(1)));

        return new Board(width,height,tiles);
    }

    /**
     * @author Weihao Mo
     */
    public static Board initBoardWithRebootTokenAndPits(int width, int height) {
        Tile[][] tiles = initEmptyCells(width,height);
        Tile cp1 = tiles[2][2];
        cp1.setEffects(List.of(new RebootToken(Direction.E)));

        Tile cp2 = tiles[4][4];
        cp2.setEffects(List.of(new Checkpoint(1)));
        Tile cp3 = tiles[0][1];
        cp3.setEffects(List.of(new Pits()));


        return new Board(width,height,tiles);
    }

    /**
     * @author Patrick Røbel
     */
    public static Board initBoardWithBoardLasers(int width, int height) {
        Tile[][] tiles = initEmptyCells(width, height);
        
        // Laser 1: At (2, 1) facing South (towards increasing Y) with power 1 - for basic damage test
        // This will hit robot at (2, 2)
        Tile laser1 = tiles[2][1];
        laser1.setEffects(List.of(new BoardLaser(Direction.S, 1)));
        
        // Laser 2: At (3, 1) facing South (towards increasing Y) with power 3 - for high power test
        // This will hit robot at (3, 2) or (3, 3)
        Tile laser2 = tiles[3][1];
        laser2.setEffects(List.of(new BoardLaser(Direction.S, 3)));
        
        // Wall at (1, 2) blocking North side - used for wall blocking test
        Tile wall = tiles[1][2];
        wall.setEffects(List.of(new Walls(EnumSet.of(Direction.N))));
        
        // Laser 3: At (1, 1) facing South with power 2 - will be blocked by wall at (1, 2)
        Tile laser3blocked = tiles[1][1];
        laser3blocked.setEffects(List.of(new BoardLaser(Direction.S, 2)));
        
        // Laser 4: At (2, 5) facing East with power 2 - for multiple robots test
        Tile laser4 = tiles[2][5];
        laser4.setEffects(List.of(new BoardLaser(Direction.E, 2)));
        
        Tile cp = tiles[8][8];
        cp.setEffects(List.of(new Checkpoint(1)));
        
        return new Board(width, height, tiles);
    }
}
