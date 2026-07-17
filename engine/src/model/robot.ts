import { Direction } from "./direction.js";
import type { ProgramCard } from "../program/programCard.js";
import type { ProgramOP } from "../program/programOp.js";

/** Ported from dk.dtu.domain.model.Robot. */
export class Robot {
  readonly id: number;
  private x: number;
  private y: number;
  private direction: Direction;
  private nextCheckpoint = 1;
  private movedOnActivationFlag = false;
  private alive = true;
  private respawnDirection: Direction | null = null;
  private lastExecutedOp: ProgramOP | null = null;

  private readonly registers: ProgramOP[] = [];
  private readonly pcRegisters: ProgramCard[] = [];

  constructor(
    id: number,
    x: number,
    y: number,
    direction: Direction,
    nextCheckpoint = 1,
  ) {
    this.id = id;
    this.x = x;
    this.y = y;
    this.direction = direction;
    this.nextCheckpoint = nextCheckpoint;
  }

  loadProgram(cards: ProgramCard[]): void {
    this.registers.length = 0;
    this.pcRegisters.length = 0;
    for (const c of cards) {
      this.registers.push(...c.toOps());
      this.pcRegisters.push(c);
    }
  }

  pollNextOp(): ProgramOP | null {
    return this.registers.shift() ?? null;
  }

  pollNextPc(): ProgramCard | null {
    return this.pcRegisters.shift() ?? null;
  }

  hasPendingOps(): boolean {
    return this.registers.length > 0;
  }

  getLastExecutedOp(): ProgramOP | null {
    return this.lastExecutedOp;
  }

  setLastExecutedOp(op: ProgramOP): void {
    this.lastExecutedOp = op;
  }

  getNextCheckpoint(): number {
    return this.nextCheckpoint;
  }

  advanceCheckpointIfMatches(checkpointNumber: number): void {
    if (checkpointNumber === this.nextCheckpoint) {
      this.nextCheckpoint++;
    }
  }

  hasWon(totalCheckpoints: number): boolean {
    return this.nextCheckpoint > totalCheckpoints;
  }

  getId(): number {
    return this.id;
  }

  getX(): number {
    return this.x;
  }

  getY(): number {
    return this.y;
  }

  setX(x: number): void {
    this.x = x;
  }

  setY(y: number): void {
    this.y = y;
  }

  getDirection(): Direction {
    return this.direction;
  }

  setDirection(direction: Direction): void {
    this.direction = direction;
  }

  setPosition(x: number, y: number): void {
    this.x = x;
    this.y = y;
  }

  clearRegisters(): void {
    this.registers.length = 0;
    this.pcRegisters.length = 0;
    this.lastExecutedOp = null;
  }

  movedOnActivation(): boolean {
    return this.movedOnActivationFlag;
  }

  setMovedOnActivation(v: boolean): void {
    this.movedOnActivationFlag = v;
  }

  setAlive(): void {
    this.alive = true;
  }

  setDead(): void {
    this.alive = false;
  }

  isAlive(): boolean {
    return this.alive;
  }

  setRespawnDirection(direction: Direction): void {
    this.respawnDirection = direction;
  }

  getRespawnDirection(): Direction | null {
    return this.respawnDirection;
  }

  clearRespawnDirection(): void {
    this.respawnDirection = null;
  }
}
