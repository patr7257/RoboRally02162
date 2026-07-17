import { Direction } from "../model/direction.js";
import { Deck } from "../model/deck.js";
import { BoardApiImpl } from "../rules/boardApi.js";
import { Game } from "../core/game.js";
import {
  BoardSnapshot,
  CardSnapshot,
  DeckSnapshot,
  GameSnapshot,
  PlayerSnapshot,
  boardFromSnapshot,
  boardToSnapshot,
  cardFromSnapshot,
  damageDecksFromSnapshot,
  damageDecksToSnapshot,
  deckFromSnapshot,
  deckToSnapshot,
  robotFromSnapshot,
  robotToSnapshot,
} from "./snapshot.js";

/**
 * Host-authoritative game orchestrator (the browser equivalent of the Java
 * GameManager). Pure and serializable: every operation takes a GameSnapshot
 * and returns a new one, so the lobby-creator can PUT the result to the Vercel
 * backend and resume from it. runActivation additionally returns per-update
 * animation frames for streaming to players over SSE.
 */

export interface HostPlayerConfig {
  robotId: number;
  name: string;
  color: string;
  x: number;
  y: number;
  facing: Direction;
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
}

export interface ActivationResult {
  snapshot: GameSnapshot;
  frames: Frame[];
}

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
  };
}

export function submitProgram(
  snapshot: GameSnapshot,
  robotId: number,
  cards: CardSnapshot[],
): GameSnapshot {
  const next = cloneSnapshot(snapshot);
  const player = next.players.find((p) => p.robotId === robotId);
  if (!player) throw new Error("No player for robot " + robotId);
  player.program = cards.map((c) => ({ action: c.action, steps: c.steps }));
  player.locked = true;
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

/**
 * Rebuilds the engine from the snapshot, runs one full round using each
 * player's locked program, and returns the next snapshot plus the animation
 * frames captured on every engine update.
 */
export function runActivation(snapshot: GameSnapshot): ActivationResult {
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
    if (p.program) {
      const robot = game.getRobot(p.robotId);
      if (robot) robot.loadProgram(p.program.map(cardFromSnapshot));
    }
  }

  const frames: Frame[] = [];
  const capture = () => {
    frames.push({
      robots: game.getRobots().map((r) => ({
        id: r.getId(),
        x: r.getX(),
        y: r.getY(),
        facing: r.getDirection(),
        alive: r.isAlive(),
      })),
    });
  };

  game.addObserver({
    onWinnerDeclared() {},
    onGameUpdate() {
      capture();
    },
  });

  capture(); // initial frame before any movement
  game.startRound();

  const winner = game.getWinner();
  const decks: Record<string, DeckSnapshot> = {};
  for (const [robotId, deck] of game.getDeckMap()) {
    decks[String(robotId)] = deckToSnapshot(deck);
  }

  const next: GameSnapshot = {
    status: winner !== null ? "finished" : "programming",
    round: snapshot.round + 1,
    board: boardToSnapshot(board),
    robots: game.getRobots().map(robotToSnapshot),
    decks,
    damageDecks: damageDecksToSnapshot(game.getDamageDecks()),
    players: snapshot.players.map((p) => ({
      ...p,
      program: null,
      locked: false,
    })),
    winner,
  };

  return { snapshot: next, frames };
}
