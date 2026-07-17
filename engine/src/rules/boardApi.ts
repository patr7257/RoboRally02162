import { Board } from "../model/board.js";
import { Tile } from "../model/tile.js";
import { Robot } from "../model/robot.js";
import { Direction, fromDelta, opposite } from "../model/direction.js";
import { Rotation } from "../model/rotation.js";
import { turnLeft, turnRight } from "../model/direction.js";
import { Coord, Edge } from "./coord.js";
import {
  BeltIntent,
  Blocked,
  BlockReason,
  DestroyCause,
  DestroyEvent,
  EdgeBlock,
  Moved,
  MoveEvent,
  Outcome,
  RobotChainImmovable,
} from "./outcome.js";
import { Walls } from "../effects/walls.js";
import { BlueConveyor, GreenConveyor } from "../effects/conveyors.js";
import { Gear } from "../effects/gear.js";

/**
 * Board engine surface used by tile effects and the Game.
 * Ported from dk.dtu.domain.rules.api.BoardAPI.
 */
export interface BoardAPI {
  next(from: Coord, dir: Direction): Coord;
  hasWallBetween(from: Coord, to: Coord): boolean;
  isInBounds(x: number, y: number): boolean;
  getTile(x: number, y: number): Tile;
  getRobotsOnTile(x: number, y: number): Robot[];
  getRobots(): Robot[];
  getRobotsByPriority(): Robot[];
  updatePriorityList(order: number[]): void;
  addIntent(intent: BeltIntent): void;
  resolveIntents(): Outcome;
  tryMoveOneStep(robotId: number, dir: Direction): Outcome;
  reportDestroy(
    robotId: number,
    at: Coord,
    cause: DestroyCause,
    power?: number,
  ): void;
  notifyTileEffectActivated(x: number, y: number, effectKind: string): void;
}

/** Ported from dk.dtu.domain.rules.api.BoardApiImpl. */
export class BoardApiImpl implements BoardAPI {
  private readonly board: Board;
  private readonly robots: Map<number, Robot>;
  private readonly intents: BeltIntent[] = [];
  private priorityOrder: number[] = [];
  private readonly pendingDestroys: DestroyEvent[] = [];

  constructor(board: Board, robots: Robot[]) {
    this.board = board;
    this.robots = new Map();
    for (const robot of robots) {
      this.robots.set(robot.getId(), robot);
    }
  }

  next(from: Coord, dir: Direction): Coord {
    let nx = from.x;
    if (dir === Direction.E) nx = from.x + 1;
    else if (dir === Direction.W) nx = from.x - 1;
    let ny = from.y;
    if (dir === Direction.S) ny = from.y + 1;
    else if (dir === Direction.N) ny = from.y - 1;
    return new Coord(nx, ny);
  }

  hasWallBetween(from: Coord, to: Coord): boolean {
    if (!from.isAdjacentTo(to)) {
      return false;
    }
    const dir = fromDelta(from.x, from.y, to.x, to.y);
    if (dir === null) throw new Error("Not orthogonal neighbors");

    let hasWall = false;
    if (this.board.isInBounds(from.x, from.y)) {
      const t1 = this.board.getTile(from.x, from.y);
      if (Walls.hasWall(t1, dir)) hasWall = true;
    }
    if (this.board.isInBounds(to.x, to.y)) {
      const t2 = this.board.getTile(to.x, to.y);
      if (Walls.hasWall(t2, opposite(dir))) hasWall = true;
    }
    return hasWall;
  }

  private robotAt(c: Coord): Robot | null {
    for (const r of this.robots.values()) {
      if (r.getX() === c.x && r.getY() === c.y && r.isAlive()) return r;
    }
    return null;
  }

  tryMoveOneStep(robotId: number, dir: Direction): Outcome {
    const mover = this.robots.get(robotId)!;

    const moves: MoveEvent[] = [];
    const destroys: DestroyEvent[] = [];

    const from = new Coord(mover.getX(), mover.getY());
    const adj = this.next(from, dir);
    if (this.hasWallBetween(from, adj)) {
      return new Blocked(new EdgeBlock(new Edge(from, adj)));
    }

    if (!this.board.isInBounds(adj.x, adj.y)) {
      destroys.push(new DestroyEvent(mover.getId(), adj, DestroyCause.FELL_OFF));
      return new Moved([...moves], [...destroys]);
    }

    const chain: Robot[] = [];
    let prev = from;
    let pos = adj;

    for (;;) {
      if (this.hasWallBetween(prev, pos)) {
        const chainIds = chain.map((r) => r.getId());
        return new Blocked(
          new RobotChainImmovable(chainIds, new EdgeBlock(new Edge(prev, pos))),
        );
      }

      const r = this.robotAt(pos);
      if (r === null) break;

      chain.push(r);
      prev = pos;
      pos = this.next(pos, dir);
    }

    const tailOffBoard = !this.board.isInBounds(pos.x, pos.y);

    if (chain.length > 0) {
      const tail = chain[chain.length - 1];
      const tailFrom = new Coord(tail.getX(), tail.getY());

      if (tailOffBoard) {
        destroys.push(new DestroyEvent(tail.getId(), pos, DestroyCause.FELL_OFF));
      } else {
        moves.push(new MoveEvent(tail.getId(), tailFrom, pos));
      }

      for (let i = chain.length - 2; i >= 0; i--) {
        const r = chain[i];
        const rFrom = new Coord(r.getX(), r.getY());
        const rTo = this.next(rFrom, dir);
        moves.push(new MoveEvent(r.getId(), rFrom, rTo));
      }
    }

    moves.push(new MoveEvent(mover.getId(), from, adj));

    return new Moved([...moves], [...destroys]);
  }

  addIntent(intent: BeltIntent): void {
    this.intents.push(intent);
  }

  resolveIntents(): Outcome {
    if (this.intents.length === 0 && this.pendingDestroys.length === 0) {
      return new Moved([], []);
    }

    const allMoves: MoveEvent[] = [];
    const allDestroys: DestroyEvent[] = [];

    const byPriority = new Map<number, BeltIntent[]>();
    for (const intent of this.intents) {
      const list = byPriority.get(intent.priority) ?? [];
      list.push(intent);
      byPriority.set(intent.priority, list);
    }

    const priorities = [...byPriority.keys()].sort((a, b) => b - a);

    for (const priority of priorities) {
      const priorityIntents = byPriority.get(priority)!;
      this.processPriorityGroup(priorityIntents, priority, allMoves, allDestroys);
    }
    allDestroys.push(...this.pendingDestroys);

    this.intents.length = 0;
    this.pendingDestroys.length = 0;
    return new Moved(allMoves, allDestroys);
  }

  private processPriorityGroup(
    intents: BeltIntent[],
    priority: number,
    allMoves: MoveEvent[],
    allDestroys: DestroyEvent[],
  ): void {
    if (intents.length === 0) return;

    const maxSteps = intents[0].speed;

    let movingRobots: Robot[] = [];
    for (const intent of intents) {
      const r = this.robots.get(intent.robotId);
      if (r && r.isAlive() && !r.movedOnActivation()) {
        movingRobots.push(r);
      }
    }

    const blockedByCollision = this.predictCollisions(
      movingRobots,
      maxSteps,
      priority,
    );

    for (let step = 0; step < maxSteps; step++) {
      const robotsToMove: Robot[] = [];
      for (const r of movingRobots) {
        if (
          r.isAlive() &&
          !blockedByCollision.has(r.getId()) &&
          this.isRobotOnConveyor(r, priority)
        ) {
          robotsToMove.push(r);
        }
      }

      if (robotsToMove.length === 0) break;

      const originalPos = new Map<number, Coord>();
      for (const r of robotsToMove) {
        originalPos.set(r.getId(), new Coord(r.getX(), r.getY()));
      }

      const outcomes = new Map<Robot, Outcome>();
      const movingIds = new Set<number>();

      for (const robot of robotsToMove) {
        movingIds.add(robot.getId());
        const dir = this.getConveyorDirection(robot, priority);
        if (dir !== null) {
          outcomes.set(robot, this.tryMoveOneStep(robot.getId(), dir));
        }
      }

      const canMove = new Set<Robot>();
      for (const robot of robotsToMove) {
        const outcome = outcomes.get(robot);
        if (outcome instanceof Moved) {
          canMove.add(robot);
        } else if (outcome instanceof Blocked) {
          if (outcome.reason instanceof RobotChainImmovable) {
            const allMoving = outcome.reason.chain.every((id) =>
              movingIds.has(id),
            );
            if (allMoving) {
              canMove.add(robot);
            }
          }
        }
      }

      const processedIds = new Set<number>();
      const movedThisStep: Robot[] = [];

      for (const robot of canMove) {
        if (!robot.isAlive()) continue;

        const outcome = outcomes.get(robot);
        if (outcome instanceof Moved) {
          for (const destroy of outcome.destroys) {
            const r = this.robots.get(destroy.robotId);
            if (r) {
              r.setDead();
              processedIds.add(r.getId());
            }
            allDestroys.push(destroy);
          }

          for (const move of outcome.moves) {
            const r = this.robots.get(move.robotId);
            if (r && !processedIds.has(r.getId())) {
              if (r === robot || !movingIds.has(r.getId())) {
                r.setPosition(move.to.x, move.to.y);
                processedIds.add(r.getId());
                allMoves.push(move);
              }
            }
          }

          if (robot.isAlive()) {
            movedThisStep.push(robot);
          }
        }
      }

      for (const robot of movedThisStep) {
        robot.setMovedOnActivation(true);
      }

      for (const robot of movedThisStep) {
        if (robot.isAlive()) {
          const oldPos = originalPos.get(robot.getId())!;
          const newPos = new Coord(robot.getX(), robot.getY());
          if (!oldPos.equals(newPos)) {
            this.applyRotation(robot, priority);
          }
        }
      }

      movingRobots = movedThisStep;
    }

    for (const robotId of blockedByCollision) {
      const r = this.robots.get(robotId);
      if (r) {
        r.setMovedOnActivation(true);
      }
    }
  }

  private predictCollisions(
    robotList: Robot[],
    maxSteps: number,
    priority: number,
  ): Set<number> {
    const simPos = new Map<number, Coord>();
    for (const r of robotList) {
      simPos.set(r.getId(), new Coord(r.getX(), r.getY()));
    }

    const colliding = new Set<number>();

    for (let step = 0; step < maxSteps; step++) {
      const nextDest = new Map<string, number[]>();

      for (const robot of robotList) {
        if (colliding.has(robot.getId())) continue;

        const pos = simPos.get(robot.getId())!;
        if (!this.isInBounds(pos.x, pos.y)) continue;

        const conveyorDir = this.getConveyorDirectionAt(pos, priority);
        if (conveyorDir === null) continue;

        const nextPos = this.next(pos, conveyorDir);
        if (this.hasWallBetween(pos, nextPos)) continue;
        if (!this.isInBounds(nextPos.x, nextPos.y)) continue;

        const key = `${nextPos.x},${nextPos.y}`;
        const list = nextDest.get(key) ?? [];
        list.push(robot.getId());
        nextDest.set(key, list);
      }

      for (const ids of nextDest.values()) {
        if (ids.length > 1) {
          for (const id of ids) colliding.add(id);
        }
      }

      for (const [key, ids] of nextDest) {
        if (ids.length === 1) {
          const [xs, ys] = key.split(",");
          simPos.set(ids[0], new Coord(Number(xs), Number(ys)));
        }
      }
    }

    return colliding;
  }

  private getConveyorDirectionAt(pos: Coord, priority: number): Direction | null {
    if (!this.isInBounds(pos.x, pos.y)) return null;
    const tile = this.getTile(pos.x, pos.y);
    for (const eff of tile.getEffects()) {
      if (priority === 2 && eff instanceof BlueConveyor) return eff.direction;
      if (priority === 1 && eff instanceof GreenConveyor) return eff.direction;
    }
    return null;
  }

  private isRobotOnConveyor(robot: Robot, priority: number): boolean {
    if (!this.isInBounds(robot.getX(), robot.getY())) return false;
    const tile = this.getTile(robot.getX(), robot.getY());
    for (const eff of tile.getEffects()) {
      if (priority === 2 && eff instanceof BlueConveyor) return true;
      if (priority === 1 && eff instanceof GreenConveyor) return true;
    }
    return false;
  }

  private getConveyorDirection(robot: Robot, priority: number): Direction | null {
    if (!this.isInBounds(robot.getX(), robot.getY())) return null;
    const tile = this.getTile(robot.getX(), robot.getY());
    for (const eff of tile.getEffects()) {
      if (priority === 2 && eff instanceof BlueConveyor) return eff.direction;
      if (priority === 1 && eff instanceof GreenConveyor) return eff.direction;
    }
    return null;
  }

  private applyRotation(robot: Robot, priority: number): void {
    if (!this.isInBounds(robot.getX(), robot.getY())) return;
    const tile = this.getTile(robot.getX(), robot.getY());
    for (const eff of tile.getEffects()) {
      let rot: Rotation | null = null;
      if (priority === 2 && eff instanceof BlueConveyor) rot = eff.rotation;
      if (priority === 1 && eff instanceof GreenConveyor) rot = eff.rotation;
      if (eff instanceof Gear) rot = eff.rotation;

      if (rot !== null && rot !== Rotation.NONE) {
        if (rot === Rotation.LEFT) {
          robot.setDirection(turnLeft(robot.getDirection()));
        } else if (rot === Rotation.RIGHT) {
          robot.setDirection(turnRight(robot.getDirection()));
        }
        return;
      }
    }
  }

  getRobotsOnTile(x: number, y: number): Robot[] {
    const result: Robot[] = [];
    for (const r of this.robots.values()) {
      if (r.getX() === x && r.getY() === y && r.isAlive()) {
        result.push(r);
      }
    }
    return result;
  }

  getTile(x: number, y: number): Tile {
    return this.board.getTile(x, y);
  }

  isInBounds(x: number, y: number): boolean {
    return this.board.isInBounds(x, y);
  }

  getRobots(): Robot[] {
    return [...this.robots.values()];
  }

  updatePriorityList(order: number[]): void {
    this.priorityOrder = [...order];
  }

  getRobotsByPriority(): Robot[] {
    const sorted: Robot[] = [];
    for (const robotId of this.priorityOrder) {
      const robot = this.robots.get(robotId);
      if (robot) sorted.push(robot);
    }
    for (const robot of this.robots.values()) {
      if (!sorted.includes(robot)) sorted.push(robot);
    }
    return sorted;
  }

  reportDestroy(
    robotId: number,
    at: Coord,
    cause: DestroyCause,
    power = 0,
  ): void {
    this.pendingDestroys.push(new DestroyEvent(robotId, at, cause, power));
  }

  notifyTileEffectActivated(_x: number, _y: number, _effectKind: string): void {
    // Animation hook; no listeners in the headless engine.
  }
}

export type { BlockReason };
