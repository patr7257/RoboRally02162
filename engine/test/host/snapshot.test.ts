import { describe, it, expect } from "vitest";
import { Board } from "../../src/model/board.js";
import { Tile } from "../../src/model/tile.js";
import { Robot } from "../../src/model/robot.js";
import { Deck } from "../../src/model/deck.js";
import { DamageDecks } from "../../src/model/damageDecks.js";
import { Direction } from "../../src/model/direction.js";
import { Rotation } from "../../src/model/rotation.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { Walls } from "../../src/effects/walls.js";
import { Checkpoint } from "../../src/effects/checkpoint.js";
import { GreenConveyor, BlueConveyor } from "../../src/effects/conveyors.js";
import { Gear } from "../../src/effects/gear.js";
import { Pits } from "../../src/effects/pits.js";
import { RebootToken } from "../../src/effects/rebootToken.js";
import { Antenna } from "../../src/effects/antenna.js";
import { StartingTile } from "../../src/effects/startingTile.js";
import { BoardLaser } from "../../src/effects/boardLaser.js";
import {
  boardToSnapshot,
  boardFromSnapshot,
  robotToSnapshot,
  robotFromSnapshot,
  deckToSnapshot,
  deckFromSnapshot,
  opToCardSnapshot,
} from "../../src/host/snapshot.js";
import {
  AgainOp,
  MoveOp,
  ReactionOp,
  RotateLeftOp,
  RotateRightOp,
  SpamOp,
  UTurnOp,
} from "../../src/program/programOp.js";

function richBoard(): Board {
  const w = 4;
  const h = 3;
  const tiles: Tile[][] = [];
  for (let x = 0; x < w; x++) {
    tiles[x] = [];
    for (let y = 0; y < h; y++) tiles[x][y] = new Tile(x, y, []);
  }
  tiles[0][0].setEffects([
    new Walls([Direction.N, Direction.E]),
    new Checkpoint(1),
  ]);
  tiles[1][0].setEffects([new GreenConveyor(Direction.S, Rotation.RIGHT)]);
  tiles[2][0].setEffects([new BlueConveyor(Direction.E, Rotation.NONE)]);
  tiles[3][0].setEffects([new Gear(Rotation.LEFT)]);
  tiles[0][1].setEffects([new Pits()]);
  tiles[1][1].setEffects([new RebootToken(Direction.W)]);
  tiles[2][1].setEffects([new Antenna(Direction.N)]);
  tiles[3][1].setEffects([new StartingTile(2)]);
  tiles[0][2].setEffects([new BoardLaser(Direction.S, 2)]);
  return new Board(w, h, tiles);
}

describe("snapshot round-trip", () => {
  it("board survives snapshot -> engine -> snapshot unchanged", () => {
    const original = boardToSnapshot(richBoard());
    const restored = boardToSnapshot(boardFromSnapshot(original));
    expect(restored).toEqual(original);
  });

  it("restored board keeps effect behaviour (a checkpoint still counts)", () => {
    const board = boardFromSnapshot(boardToSnapshot(richBoard()));
    const tile = board.getTile(0, 0);
    const kinds = tile.getEffects().map((e) => e.constructor.name);
    expect(kinds).toContain("Walls");
    expect(kinds).toContain("Checkpoint");
  });

  it("robot survives round-trip including alive/respawn state", () => {
    const r = new Robot(3, 4, 5, Direction.W, 2);
    r.setRespawnDirection(Direction.S);
    r.setDead();
    const snap = robotToSnapshot(r);
    const restored = robotToSnapshot(robotFromSnapshot(snap));
    expect(restored).toEqual(snap);
    expect(snap.alive).toBe(false);
    expect(snap.respawnDirection).toBe(Direction.S);
  });

  it("a robot without activation state keeps the optional fields absent", () => {
    const snap = robotToSnapshot(new Robot(1, 0, 0, Direction.N));
    expect("registers" in snap).toBe(false);
    expect("lastExecuted" in snap).toBe(false);
    expect("movedOnActivation" in snap).toBe(false);
    expect(robotToSnapshot(robotFromSnapshot(snap))).toEqual(snap);
  });

  it("a robot round-trips its mid-activation state", () => {
    const r = new Robot(2, 1, 1, Direction.E);
    r.loadProgram([ProgramCard.move1(), ProgramCard.left()]);
    r.pollNextOp();
    r.pollNextPc();
    r.setLastExecutedOp(new MoveOp(1));
    r.setMovedOnActivation(true);

    const snap = robotToSnapshot(r, true);
    expect(snap.registers).toEqual([{ action: "ROTATELEFT", steps: 0 }]);
    expect(snap.lastExecuted).toEqual({ action: "MOVE", steps: 1 });
    expect(snap.movedOnActivation).toBe(true);

    const restored = robotFromSnapshot(snap);
    expect(robotToSnapshot(restored, true)).toEqual(snap);
    expect(restored.peekNextPc()?.toString()).toBe("ROTATELEFT");
  });

  it("opToCardSnapshot covers the concrete ops and rejects the others", () => {
    expect(opToCardSnapshot(new MoveOp(-1))).toEqual({ action: "MOVE", steps: -1 });
    expect(opToCardSnapshot(new RotateRightOp())).toEqual({
      action: "ROTATERIGHT",
      steps: 0,
    });
    expect(opToCardSnapshot(new RotateLeftOp())).toEqual({
      action: "ROTATELEFT",
      steps: 0,
    });
    expect(opToCardSnapshot(new UTurnOp())).toEqual({ action: "UTURN", steps: 0 });
    expect(() => opToCardSnapshot(new SpamOp())).toThrowError(/concrete program op/);
    expect(() => opToCardSnapshot(new AgainOp())).toThrowError(/concrete program op/);
    expect(() => opToCardSnapshot(new ReactionOp("SANDBOX"))).toThrowError(
      /concrete program op/,
    );
  });

  it("deck survives round-trip preserving piles", () => {
    const dd = new DamageDecks(38, 15, 15);
    const deck = new Deck(
      [ProgramCard.move1(), ProgramCard.left()],
      [ProgramCard.uturn()],
      [ProgramCard.spam(), ProgramCard.move2()],
      dd,
    );
    const snap = deckToSnapshot(deck);
    const restored = deckToSnapshot(deckFromSnapshot(snap, dd));
    expect(restored).toEqual(snap);
    expect(snap.hand).toContainEqual({ action: "SPAM", steps: 0 });
    expect(snap.drawPile).toContainEqual({ action: "MOVE", steps: 1 });
  });
});
