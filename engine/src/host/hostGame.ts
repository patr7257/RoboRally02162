import { Direction } from "../model/direction.js";
import { Deck } from "../model/deck.js";
import { Board } from "../model/board.js";
import { BoardApiImpl } from "../rules/boardApi.js";
import { Game } from "../core/game.js";
import type { ReactionKind } from "../program/programOp.js";
import {
  REACTION_SPECS,
  normalizeChoice,
  type ReactionChoice,
} from "../program/reaction.js";
import {
  ActivationCursor,
  BoardSnapshot,
  CardSnapshot,
  DeckSnapshot,
  GameSnapshot,
  GameStatus,
  PendingReaction,
  PlayerSnapshot,
  boardFromSnapshot,
  boardToSnapshot,
  cardFromSnapshot,
  cardToSnapshot,
  damageDecksFromSnapshot,
  damageDecksToSnapshot,
  deckFromSnapshot,
  deckToSnapshot,
  robotFromSnapshot,
  robotToSnapshot,
} from "./snapshot.js";

/**
 * Host-authoritative game orchestrator (the browser equivalent of the Java
 * GameManager plus GameScheduler). Pure and serializable: every operation takes
 * a GameSnapshot and returns a new one, so the lobby-creator can PUT the result
 * to the Vercel backend and resume from it. runActivation and its resume
 * siblings additionally return per-update animation frames for streaming to
 * players over SSE.
 *
 * An activation can pause twice over:
 *   - "awaiting-reaction": an interactive card (SANDBOX / WEASEL / SPEED) needs
 *     a choice. Resume with resumeActivation.
 *   - "awaiting-respawn": robots died during the round and need a facing before
 *     new hands are dealt. Resume with applyRespawns.
 * Nothing before a pause is ever re-executed on resume: the deck randomness of
 * the completed part is baked into the paused snapshot.
 */

export { REACTION_SPECS };
export type { ReactionChoice, ReactionKind };

/**
 * A reaction with a single legal option carries no decision, so the host
 * applies it immediately instead of prompting (SPEED -> MOVE3).
 */
const AUTO_RESOLVE_SINGLE_OPTION = true;

export interface HostPlayerConfig {
  robotId: number;
  name: string;
  color: string;
  x: number;
  y: number;
  facing: Direction;
}

/** What the robots were doing when a frame was captured (issue #7). */
export interface FrameLabel {
  /** null for board-effect and end-of-register frames. */
  robotId: number | null;
  /** 1..5 during activation, 0 during the reboot phase. */
  register: number;
  /** Card name ("MOVE2"), resolved reaction choice, "BOARD" or "REBOOT". */
  text: string;
}

/** One animation frame: robot positions/headings at a point during activation. */
export interface Frame {
  robots: {
    id: number;
    x: number;
    y: number;
    facing: Direction;
    alive: boolean;
  }[];
  label?: FrameLabel;
}

export interface ActivationResult {
  snapshot: GameSnapshot;
  frames: Frame[];
}

/**
 * Label stamped onto every frame the observer captures. Module-local because
 * the whole run loop is synchronous: one activation segment at a time.
 */
let currentLabel: FrameLabel | null = null;

function cloneSnapshot(snapshot: GameSnapshot): GameSnapshot {
  return JSON.parse(JSON.stringify(snapshot)) as GameSnapshot;
}

/** Deals a fresh 9-card hand for a new standard deck. */
function freshDeck(damageDecks: ReturnType<typeof damageDecksFromSnapshot>): DeckSnapshot {
  const deck = Deck.standard(damageDecks);
  deck.dealHand(9);
  return deckToSnapshot(deck);
}

export function createGame(
  board: BoardSnapshot,
  players: HostPlayerConfig[],
): GameSnapshot {
  const damageDecks = damageDecksFromSnapshot({ spam: 38, trojan: 15, worm: 15 });
  const robots = players.map((p) => ({
    id: p.robotId,
    x: p.x,
    y: p.y,
    facing: p.facing,
    nextCheckpoint: 1,
    alive: true,
    respawnDirection: null,
  }));
  const decks: Record<string, DeckSnapshot> = {};
  const playerSnaps: PlayerSnapshot[] = [];
  for (const p of players) {
    decks[String(p.robotId)] = freshDeck(damageDecks);
    playerSnaps.push({
      robotId: p.robotId,
      name: p.name,
      color: p.color,
      program: null,
      locked: false,
    });
  }
  return {
    status: "programming",
    round: 1,
    board,
    robots,
    decks,
    damageDecks: damageDecksToSnapshot(damageDecks),
    players: playerSnaps,
    winner: null,
    activation: null,
    pendingReaction: null,
  };
}

/**
 * Validates a pick against the player's hand and stores the completed program
 * (issue #14). An empty or short pick legally auto-completes to five cards off
 * the draw pile, so the drawn cards (and a reshuffle they may trigger) are
 * written back into the returned snapshot: they happen exactly once, host-side.
 * Throws when a card is not in hand, leaving the input snapshot untouched.
 */
export function submitProgram(
  snapshot: GameSnapshot,
  robotId: number,
  cards: CardSnapshot[],
): GameSnapshot {
  const next = cloneSnapshot(snapshot);
  const player = next.players.find((p) => p.robotId === robotId);
  if (!player) throw new Error("No player for robot " + robotId);
  const deckSnap = next.decks[String(robotId)];
  if (!deckSnap) throw new Error("No deck for robot " + robotId);

  const damageDecks = damageDecksFromSnapshot(next.damageDecks);
  const deck = deckFromSnapshot(deckSnap, damageDecks);
  const program = deck.validateAndCompleteOrThrow(cards.map(cardFromSnapshot));

  player.program = program.map(cardToSnapshot);
  player.locked = true;
  next.decks[String(robotId)] = deckToSnapshot(deck);
  next.damageDecks = damageDecksToSnapshot(damageDecks);
  return next;
}

/** True when every alive player has locked in a program. */
export function allSubmitted(snapshot: GameSnapshot): boolean {
  return snapshot.players.every((p) => {
    const robot = snapshot.robots.find((r) => r.id === p.robotId);
    if (robot && !robot.alive) return true;
    return p.locked;
  });
}

interface Rebuilt {
  game: Game;
  board: Board;
}

/**
 * Rebuilds the engine from a snapshot. Robots restore their own mid-activation
 * state (remaining registers, last executed op, conveyor flag) when the
 * snapshot carries it, in which case the stored player program is ignored: it
 * has already been partly consumed.
 */
function rebuild(snapshot: GameSnapshot): Rebuilt {
  const board = boardFromSnapshot(snapshot.board);
  const robots = snapshot.robots.map(robotFromSnapshot);
  const damageDecks = damageDecksFromSnapshot(snapshot.damageDecks);
  const api = new BoardApiImpl(board, robots);
  const game = new Game(board, api, robots);
  game.setDamageDecks(damageDecks);

  for (const r of robots) {
    const ds = snapshot.decks[String(r.getId())];
    if (ds) game.setDeck(deckFromSnapshot(ds, damageDecks), r.getId());
  }

  for (const p of snapshot.players) {
    const rs = snapshot.robots.find((r) => r.id === p.robotId);
    if (rs && rs.registers !== undefined) continue;
    if (p.program) {
      const robot = game.getRobot(p.robotId);
      if (robot) robot.loadProgram(p.program.map(cardFromSnapshot));
    }
  }

  return { game, board };
}

function attachCapture(game: Game, frames: Frame[]): () => void {
  const capture = () => {
    const frame: Frame = {
      robots: game.getRobots().map((r) => ({
        id: r.getId(),
        x: r.getX(),
        y: r.getY(),
        facing: r.getDirection(),
        alive: r.isAlive(),
      })),
    };
    if (currentLabel !== null) frame.label = { ...currentLabel };
    frames.push(frame);
  };
  game.addObserver({
    onWinnerDeclared() {},
    onGameUpdate() {
      capture();
    },
  });
  return capture;
}

type LoopOutcome =
  | { kind: "completed" }
  | { kind: "finished" }
  | { kind: "awaiting-respawn" }
  | {
      kind: "paused";
      cursor: ActivationCursor;
      robotId: number;
      register: number;
      turnIndex: number;
      reaction: ReactionKind;
    };

/**
 * Runs registers from the cursor to the end of the round, stopping at the first
 * reaction that needs a player decision. With a cursor the current register
 * resumes at turnIndex + 1 using the frozen turn order; without one every
 * register starts by freezing a fresh antenna-priority order.
 */
function runLoop(game: Game, start: ActivationCursor | null): LoopOutcome {
  let resumeOrder: number[] | null = start ? start.turnOrder : null;
  const resumeIndex = start ? start.turnIndex + 1 : 0;
  const firstRegister = start ? start.register : 1;

  for (let reg = firstRegister; reg <= 5; reg++) {
    let order: number[];
    let startIdx: number;
    if (resumeOrder !== null) {
      order = resumeOrder;
      startIdx = resumeIndex;
      resumeOrder = null;
    } else {
      order = game.beginRegister();
      startIdx = 0;
    }

    for (let i = startIdx; i < order.length; i++) {
      const robotId = order[i];
      const robot = game.getRobot(robotId);
      currentLabel = {
        robotId,
        register: reg,
        text: robot?.peekNextPc()?.toString() ?? "",
      };

      const reaction = game.takeTurn(robotId);
      if (reaction === null) continue;

      const spec = REACTION_SPECS[reaction];
      if (AUTO_RESOLVE_SINGLE_OPTION && spec.options.length === 1) {
        currentLabel = { robotId, register: reg, text: spec.defaultChoice };
        game.resolveReaction(robotId, spec.defaultChoice);
        continue;
      }

      currentLabel = null;
      return {
        kind: "paused",
        cursor: { register: reg, turnOrder: [...order], turnIndex: i },
        robotId,
        register: reg,
        turnIndex: i,
        reaction,
      };
    }

    currentLabel = { robotId: null, register: reg, text: "BOARD" };
    game.endRegister();
    currentLabel = null;

    if (game.getWinner() !== null) return { kind: "finished" };
  }

  if (game.getDeadRobots().length > 0) return { kind: "awaiting-respawn" };
  return { kind: "completed" };
}

interface BuildOptions {
  status: GameStatus;
  round: number;
  clearPrograms: boolean;
  activationState: boolean;
  activation: ActivationCursor | null;
  pendingReaction: PendingReaction | null;
}

function buildSnapshot(
  game: Game,
  board: Board,
  base: GameSnapshot,
  opts: BuildOptions,
): GameSnapshot {
  const decks: Record<string, DeckSnapshot> = {};
  for (const [robotId, deck] of game.getDeckMap()) {
    decks[String(robotId)] = deckToSnapshot(deck);
  }
  return {
    status: opts.status,
    round: opts.round,
    board: boardToSnapshot(board),
    robots: game.getRobots().map((r) => robotToSnapshot(r, opts.activationState)),
    decks,
    damageDecks: damageDecksToSnapshot(game.getDamageDecks()),
    players: base.players.map((p) =>
      opts.clearPrograms ? { ...p, program: null, locked: false } : { ...p },
    ),
    winner: game.getWinner(),
    activation: opts.activation,
    pendingReaction: opts.pendingReaction,
  };
}

/**
 * Turns a loop outcome into the next snapshot. The round number increments only
 * on the transition back into programming: while awaiting a reaction or a
 * respawn the round stays put.
 */
function finishSegment(
  game: Game,
  board: Board,
  base: GameSnapshot,
  outcome: LoopOutcome,
  frames: Frame[],
): ActivationResult {
  if (outcome.kind === "paused") {
    const spec = REACTION_SPECS[outcome.reaction];
    const pendingReaction: PendingReaction = {
      promptId: `r${base.round}-g${outcome.register}-t${outcome.turnIndex}-${outcome.robotId}`,
      robotId: outcome.robotId,
      register: outcome.register,
      kind: outcome.reaction,
      options: [...spec.options],
      defaultChoice: spec.defaultChoice,
    };
    return {
      snapshot: buildSnapshot(game, board, base, {
        status: "awaiting-reaction",
        round: base.round,
        clearPrograms: false,
        activationState: true,
        activation: outcome.cursor,
        pendingReaction,
      }),
      frames,
    };
  }

  if (outcome.kind === "awaiting-respawn") {
    // Java deals new hands only after every dead robot has rebooted.
    return {
      snapshot: buildSnapshot(game, board, base, {
        status: "awaiting-respawn",
        round: base.round,
        clearPrograms: false,
        activationState: false,
        activation: null,
        pendingReaction: null,
      }),
      frames,
    };
  }

  if (outcome.kind === "finished") {
    return {
      snapshot: buildSnapshot(game, board, base, {
        status: "finished",
        round: base.round,
        clearPrograms: true,
        activationState: false,
        activation: null,
        pendingReaction: null,
      }),
      frames,
    };
  }

  game.dealNewHands();
  return {
    snapshot: buildSnapshot(game, board, base, {
      status: "programming",
      round: base.round + 1,
      clearPrograms: true,
      activationState: false,
      activation: null,
      pendingReaction: null,
    }),
    frames,
  };
}

/**
 * Rebuilds the engine from the snapshot and runs the round from register 1
 * using each player's locked program, up to the end of the round or the first
 * pause.
 */
export function runActivation(snapshot: GameSnapshot): ActivationResult {
  if (
    snapshot.status === "awaiting-reaction" ||
    snapshot.status === "awaiting-respawn"
  ) {
    throw new Error(
      "runActivation cannot restart a paused activation (status " +
        snapshot.status +
        "); use resumeActivation or applyRespawns",
    );
  }

  const { game, board } = rebuild(snapshot);
  const frames: Frame[] = [];
  currentLabel = null;
  const capture = attachCapture(game, frames);
  capture(); // initial frame before any movement

  const outcome = runLoop(game, null);
  return finishSegment(game, board, snapshot, outcome, frames);
}

/**
 * Applies a reaction choice and continues the paused activation. A missing or
 * illegal choice falls back to the reaction's default, matching the Java
 * scheduler's timeout behaviour.
 */
export function resumeActivation(
  snapshot: GameSnapshot,
  choice?: ReactionChoice | null,
): ActivationResult {
  const pending = snapshot.pendingReaction;
  const cursor = snapshot.activation;
  if (snapshot.status !== "awaiting-reaction" || !pending || !cursor) {
    throw new Error("No pending reaction to resume (status " + snapshot.status + ")");
  }

  const { game, board } = rebuild(snapshot);
  const frames: Frame[] = [];
  currentLabel = null;
  const capture = attachCapture(game, frames);
  capture(); // initial frame, the paused position

  const resolved = normalizeChoice(pending.kind, choice ?? null);
  currentLabel = {
    robotId: pending.robotId,
    register: pending.register,
    text: resolved,
  };
  game.resolveReaction(pending.robotId, resolved);
  currentLabel = null;

  const outcome = runLoop(game, cursor);
  return finishSegment(game, board, snapshot, outcome, frames);
}

/**
 * Reboots every dead robot in antenna-priority order, then deals new hands and
 * returns to programming with the round advanced. A robot without an entry in
 * directions keeps the facing it died with.
 */
export function applyRespawns(
  snapshot: GameSnapshot,
  directions: Record<number, Direction>,
): ActivationResult {
  if (snapshot.status !== "awaiting-respawn") {
    throw new Error(
      "No respawns to apply (status " + snapshot.status + ")",
    );
  }

  const { game, board } = rebuild(snapshot);
  const frames: Frame[] = [];
  currentLabel = null;
  const capture = attachCapture(game, frames);
  capture(); // initial frame, robots still down

  const lookup = directions as Record<string | number, Direction | undefined>;
  const dead = game.getRobotsByPriority().filter((r) => !r.isAlive());
  for (const robot of dead) {
    const id = robot.getId();
    const facing = lookup[id] ?? lookup[String(id)] ?? robot.getDirection();
    currentLabel = { robotId: id, register: 0, text: "REBOOT" };
    game.setRespawnDirection(id, facing);
    game.applyRespawnPhase(robot);
  }
  currentLabel = null;

  game.dealNewHands();
  return {
    snapshot: buildSnapshot(game, board, snapshot, {
      status: "programming",
      round: snapshot.round + 1,
      clearPrograms: true,
      activationState: false,
      activation: null,
      pendingReaction: null,
    }),
    frames,
  };
}
