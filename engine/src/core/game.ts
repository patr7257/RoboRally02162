import { Board } from "../model/board.js";
import { Tile } from "../model/tile.js";
import { Robot } from "../model/robot.js";
import { Direction, opposite } from "../model/direction.js";
import { Phase, PHASES } from "./phase.js";
import type { GameObserver } from "./gameObserver.js";
import type { BoardAPI } from "../rules/boardApi.js";
import { Blocked, DestroyCause, Moved } from "../rules/outcome.js";
import { AgainOp, MoveOp, ProgramOP } from "../program/programOp.js";
import { Checkpoint } from "../effects/checkpoint.js";

/**
 * Minimal port of dk.dtu.domain.core.Game.
 *
 * This slice deliberately omits the card deck / damage system (draw piles,
 * hands, SPAM/TROJAN/WORM resolution and reboot penalties). None of the
 * movement, AGAIN, checkpoint or conveyor tests depend on it. Programs are
 * loaded directly onto robots via Robot.loadProgram, exactly as the ported
 * JUnit tests do.
 */
export class Game {
  private readonly board: Board;
  private readonly api: BoardAPI;
  private readonly robots: Robot[];
  private readonly robotMap = new Map<number, Robot>();
  private readonly phaseIndex: Map<Phase, Tile[]>;
  private winner: number | null = null;
  private readonly observers: GameObserver[] = [];

  constructor(board: Board, api: BoardAPI, robots: Robot[]) {
    this.board = board;
    this.api = api;
    this.robots = robots;
    for (const r of robots) {
      this.robotMap.set(r.getId(), r);
    }
    this.phaseIndex = this.buildPhaseIndex(board.getCells());
  }

  private buildPhaseIndex(tiles: Tile[][]): Map<Phase, Tile[]> {
    const temp = new Map<Phase, Set<Tile>>();
    for (const p of PHASES) temp.set(p, new Set());

    if (tiles) {
      for (const row of tiles) {
        if (!row) continue;
        for (const tile of row) {
          if (!tile) continue;
          for (const e of tile.getEffects()) {
            const phases = e.phases();
            if (!phases) continue;
            for (const p of phases) {
              temp.get(p)!.add(tile);
            }
          }
        }
      }
    }

    const idx = new Map<Phase, Tile[]>();
    for (const p of PHASES) {
      idx.set(p, [...temp.get(p)!]);
    }
    return idx;
  }

  startRound(): void {
    for (let reg = 1; reg <= 5; reg++) {
      this.executeRegister(reg);
      if (this.evaluateWinConditions()) break;
    }
    this.notifyGameUpdate();
  }

  executeRegister(_registerIndex: number): void {
    this.runPhase(Phase.ACTIVATION, () => this.executeOneRegister());
    this.evaluateWinConditions();
    this.notifyGameUpdate();
  }

  private runPhase(phase: Phase, body: () => void): void {
    if (phase === Phase.ACTIVATION) {
      for (const r of this.robots) r.setMovedOnActivation(false);
    }
    body();
    for (const sub of PHASES) {
      if (sub !== Phase.ACTIVATE_ANTENNA) {
        this.applyTileEffects(sub);
      }
    }
  }

  private executeOneRegister(): void {
    for (const r of this.api.getRobotsByPriority()) {
      this.executeOneRobotTurn(r);
      this.applyTileEffects(Phase.ACTIVATE_PITS);
    }
  }

  executeOneRobotTurn(robot: Robot): void {
    let op = robot.pollNextOp();
    robot.pollNextPc();
    if (op === null) return;

    if (op instanceof AgainOp) {
      const lastOp = robot.getLastExecutedOp();
      if (lastOp !== null && !(lastOp instanceof AgainOp)) {
        op = lastOp;
      } else {
        return;
      }
    }

    if (op instanceof MoveOp) {
      let dir = robot.getDirection();
      let steps = op.steps;
      if (steps < 0) {
        dir = opposite(dir);
        steps = -steps;
      }
      while (steps-- > 0) {
        const ok = this.applyOneStep(robot, dir);
        if (!ok) break;
      }
    } else {
      robot.setDirection(op.apply(robot.getDirection()));
    }

    if (!(op instanceof AgainOp)) {
      robot.setLastExecutedOp(op);
    }

    this.applyTileEffects(Phase.ACTIVATE_PITS);
    this.notifyGameUpdate();
  }

  private applyOneStep(robot: Robot, dir: Direction): boolean {
    const out = this.api.tryMoveOneStep(robot.getId(), dir);
    if (out instanceof Moved) {
      for (const e of out.moves) {
        this.robotMap.get(e.robotId)!.setPosition(e.to.x, e.to.y);
      }
      for (const d of out.destroys) {
        if (d.cause === DestroyCause.PITS || d.cause === DestroyCause.FELL_OFF) {
          const dead = this.robotMap.get(d.robotId)!;
          dead.setPosition(d.at.x, d.at.y);
          dead.clearRegisters();
          dead.setDead();
        }
      }
      return true;
    }
    if (out instanceof Blocked) {
      return false;
    }
    return false;
  }

  private applyTileEffects(phase: Phase): void {
    const tiles = this.phaseIndex.get(phase) ?? [];
    if (phase === Phase.ACTIVATE_ROBOT_LASERS) {
      // Robot lasers are out of scope for this slice.
    } else {
      for (const tile of tiles) {
        for (const effect of tile.getEffectsForPhase(phase)) {
          effect.onPhase(phase, tile, this.api);
        }
      }
    }

    const out = this.api.resolveIntents();
    if (out instanceof Moved) {
      for (const e of out.moves) {
        this.robotMap.get(e.robotId)!.setPosition(e.to.x, e.to.y);
      }
      for (const d of out.destroys) {
        if (d.cause === DestroyCause.LASER) {
          // Laser damage cards are out of scope for this slice.
        } else {
          const dead = this.robotMap.get(d.robotId)!;
          dead.setPosition(d.at.x, d.at.y);
          dead.clearRegisters();
          dead.setDead();
        }
      }
    }
  }

  evaluateWinConditions(): boolean {
    const totalCheckpoints = this.countCheckpoints();
    for (const [id, r] of this.robotMap) {
      if (r.hasWon(totalCheckpoints)) {
        this.declareWinner(id);
        return true;
      }
    }
    return false;
  }

  private countCheckpoints(): number {
    let count = 0;
    const tiles = this.phaseIndex.get(Phase.ACTIVATE_CHECKPOINTS) ?? [];
    for (const tile of tiles) {
      for (const effect of tile.getEffects()) {
        if (effect instanceof Checkpoint) count++;
      }
    }
    return count;
  }

  declareWinner(win: number): void {
    if (this.winner !== null) return;
    this.winner = win;
    this.notifyWinner(win);
  }

  getWinner(): number | null {
    return this.winner;
  }

  getRobot(robotID: number): Robot | undefined {
    return this.robotMap.get(robotID);
  }

  getRobots(): Robot[] {
    return this.robots;
  }

  getBoard(): Board {
    return this.board;
  }

  getRobotsByPriority(): Robot[] {
    return this.api.getRobotsByPriority();
  }

  addObserver(observer: GameObserver): void {
    this.observers.push(observer);
  }

  removeObserver(observer: GameObserver): void {
    const i = this.observers.indexOf(observer);
    if (i >= 0) this.observers.splice(i, 1);
  }

  private notifyWinner(win: number): void {
    for (const obs of this.observers) obs.onWinnerDeclared(this, win);
  }

  private notifyGameUpdate(): void {
    for (const obs of this.observers) obs.onGameUpdate(this);
  }
}

export type { ProgramOP };
