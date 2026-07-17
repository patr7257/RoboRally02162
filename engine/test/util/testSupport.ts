import { expect } from "vitest";
import { Robot } from "../../src/model/robot.js";
import { Board } from "../../src/model/board.js";
import { Direction } from "../../src/model/direction.js";
import { Walls } from "../../src/effects/walls.js";
import { Coord, Edge } from "../../src/rules/coord.js";
import {
  Blocked,
  EdgeBlock,
  Moved,
  Outcome,
  RobotChainImmovable,
} from "../../src/rules/outcome.js";

/** Mirrors dk.dtu.util.GameTestSupport.lineRobots. */
export function lineRobots(
  startId: number,
  startX: number,
  y: number,
  count: number,
  facing: Direction,
): Robot[] {
  const out: Robot[] = [];
  for (let i = 0; i < count; i++) {
    out.push(new Robot(startId + i, startX + i, y, facing));
  }
  return out;
}

/** Mirrors dk.dtu.util.GameTestSupport.walls. */
export function walls(b: Board, x: number, y: number, ...dirs: Direction[]): void {
  b.getTile(x, y).setEffects([new Walls(dirs)]);
}

export function assertPosDir(r: Robot, x: number, y: number, d: Direction): void {
  expect(r.getX(), "x").toBe(x);
  expect(r.getY(), "y").toBe(y);
  expect(r.getDirection(), "dir").toBe(d);
}

export function assertMoved(out: Outcome): Moved {
  expect(out).toBeInstanceOf(Moved);
  return out as Moved;
}

export function assertBlocked(out: Outcome): Blocked {
  expect(out).toBeInstanceOf(Blocked);
  return out as Blocked;
}

export function assertEdgeBlock(out: Outcome, expected: Edge): void {
  const b = assertBlocked(out);
  expect(b.reason).toBeInstanceOf(EdgeBlock);
  const eb = b.reason as EdgeBlock;
  expect(eb.edge.equals(expected)).toBe(true);
}

export function assertChainBlockedByEdge(
  out: Outcome,
  chain: number[],
  expectedStop: Edge,
): void {
  const b = assertBlocked(out);
  expect(b.reason).toBeInstanceOf(RobotChainImmovable);
  const rci = b.reason as RobotChainImmovable;
  expect(rci.chain).toEqual(chain);
  expect(rci.stop).toBeInstanceOf(EdgeBlock);
  expect(rci.stop.edge.equals(expectedStop)).toBe(true);
}

export function assertMove(
  moved: Moved,
  idx: number,
  robotId: number,
  from: Coord,
  to: Coord,
): void {
  const ev = moved.moves[idx];
  expect(ev.robotId).toBe(robotId);
  expect(ev.from.equals(from)).toBe(true);
  expect(ev.to.equals(to)).toBe(true);
}

export function assertDestroy(
  moved: Moved,
  idx: number,
  robotId: number,
  at: Coord,
): void {
  const ev = moved.destroys[idx];
  expect(ev.robotId).toBe(robotId);
  expect(ev.at.equals(at)).toBe(true);
}
