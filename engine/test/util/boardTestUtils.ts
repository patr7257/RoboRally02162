import { Board } from "../../src/model/board.js";
import { Tile } from "../../src/model/tile.js";
import { Direction } from "../../src/model/direction.js";
import { Rotation } from "../../src/model/rotation.js";
import { Checkpoint } from "../../src/effects/checkpoint.js";
import { GreenConveyor, BlueConveyor } from "../../src/effects/conveyors.js";
import { Walls } from "../../src/effects/walls.js";
import { RebootToken } from "../../src/effects/rebootToken.js";
import { Pits } from "../../src/effects/pits.js";

/** Mirrors dk.dtu.util.BoardTestUtils. */
export function initEmptyCells(width: number, height: number): Tile[][] {
  const tiles: Tile[][] = [];
  for (let x = 0; x < width; x++) {
    tiles[x] = [];
    for (let y = 0; y < height; y++) {
      tiles[x][y] = new Tile(x, y, []);
    }
  }
  return tiles;
}

export function initEmptyBoard(width: number, height: number): Board {
  return new Board(width, height, initEmptyCells(width, height));
}

export function initBoardWithCheckPoints(width: number, height: number): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][1].setEffects([new Checkpoint(1)]);
  tiles[2][2].setEffects([new Checkpoint(2)]);
  return new Board(width, height, tiles);
}

export function initBoardWithCheckPointsInDifferentNumber(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][1].setEffects([new Checkpoint(2)]);
  tiles[2][2].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithCheckPointsInThreeDifferentNumber(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][1].setEffects([new Checkpoint(1)]);
  tiles[1][2].setEffects([new Checkpoint(2)]);
  tiles[2][2].setEffects([new Checkpoint(3)]);
  return new Board(width, height, tiles);
}

export function initBoardWithBlueConveyors(width: number, height: number): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new BlueConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[3][1].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[3][2].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[3][3].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[3][4].setEffects([new BlueConveyor(Direction.E, Rotation.LEFT)]);
  tiles[4][4].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[5][4].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[6][4].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[7][4].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[8][8].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithGreenConveyors(width: number, height: number): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new GreenConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[2][1].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[2][2].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[2][3].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[4][4].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithGreenConveyorsWithCheckpoints(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new GreenConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[3][1].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[3][2].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[3][3].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithBlueConveyorsWithCheckpoints(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[4][0].setEffects([new BlueConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[4][1].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[4][2].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[4][3].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[4][4].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[4][5].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithGreenConveyorCollision(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new GreenConveyor(Direction.W, Rotation.NONE)]);
  return new Board(width, height, tiles);
}

export function initBoardWithBlueConveyorCollision(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new BlueConveyor(Direction.W, Rotation.NONE)]);
  tiles[4][0].setEffects([new BlueConveyor(Direction.W, Rotation.NONE)]);
  return new Board(width, height, tiles);
}

export function initBoardWithThreeRobotCollision(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][1].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][1].setEffects([new BlueConveyor(Direction.W, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  return new Board(width, height, tiles);
}

export function initBoardWithSecondStepCollision(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new BlueConveyor(Direction.W, Rotation.NONE)]);
  tiles[4][0].setEffects([new BlueConveyor(Direction.W, Rotation.NONE)]);
  return new Board(width, height, tiles);
}

export function initBoardWithBlueConveyorCancelMove(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  return new Board(width, height, tiles);
}

export function initBoardWithGreenConveyorAndWalls(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[1][1].setEffects([new Walls([Direction.N])]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithBlueConveyorAndWalls(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[1][1].setEffects([new Walls([Direction.N])]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithCurvedConveyorAtDestination(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][0].setEffects([new GreenConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[1][1].setEffects([new GreenConveyor(Direction.S, Rotation.NONE)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithCurvedConveyorAtDestinationBlue(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[1][0].setEffects([new BlueConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[1][1].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[1][2].setEffects([new BlueConveyor(Direction.S, Rotation.NONE)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithStraightThenCurvedGreenConveyor(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new GreenConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithStraightThenCurvedBlueConveyor(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithStraightBlue(width: number, height: number): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithStraightGreen(width: number, height: number): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[2][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithCurvedToCurvedGreenConveyor(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new GreenConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new GreenConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[1][1].setEffects([new GreenConveyor(Direction.W, Rotation.RIGHT)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithMultipleCurvedBlueConveyors(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[0][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[1][0].setEffects([new BlueConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[1][1].setEffects([new BlueConveyor(Direction.W, Rotation.RIGHT)]);
  tiles[6][6].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithRebootToken(width: number, height: number): Board {
  const tiles = initEmptyCells(width, height);
  tiles[2][2].setEffects([new RebootToken(Direction.E)]);
  tiles[4][4].setEffects([new Checkpoint(1)]);
  return new Board(width, height, tiles);
}

export function initBoardWithRebootTokenAndPits(
  width: number,
  height: number,
): Board {
  const tiles = initEmptyCells(width, height);
  tiles[2][2].setEffects([new RebootToken(Direction.E)]);
  tiles[4][4].setEffects([new Checkpoint(1)]);
  tiles[0][1].setEffects([new Pits()]);
  return new Board(width, height, tiles);
}
