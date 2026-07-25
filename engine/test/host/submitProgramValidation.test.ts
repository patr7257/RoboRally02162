import { describe, it, expect } from "vitest";
import { Direction } from "../../src/model/direction.js";
import { ProgramCard } from "../../src/program/programCard.js";
import { CardSnapshot } from "../../src/host/snapshot.js";
import { createGame, submitProgram } from "../../src/host/hostGame.js";
import {
  boardWithFarCheckpoint,
  prog,
  withDiscardPile,
  withDrawPile,
  withHand,
} from "../util/hostTestUtils.js";

/**
 * Host-side program validation (issue #14): a pick must come out of the hand,
 * a short pick auto-completes off the draw pile, and every deck mutation the
 * auto-complete causes is persisted in the returned snapshot.
 */

const sorted = (cards: CardSnapshot[]) =>
  [...cards].sort((a, b) =>
    a.action === b.action ? a.steps - b.steps : a.action < b.action ? -1 : 1,
  );

function game() {
  return createGame(boardWithFarCheckpoint(6, 6), [
    { robotId: 1, name: "Ada", color: "#f00", x: 0, y: 0, facing: Direction.E },
  ]);
}

describe("submitProgram validation", () => {
  it("throws for a card that is not in hand and leaves the input alone", () => {
    let snap = game();
    snap = withHand(snap, 1, [
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
    ]);
    const before = JSON.parse(JSON.stringify(snap));

    expect(() => submitProgram(snap, 1, prog(ProgramCard.move1()))).toThrowError(
      /Not enough MOVE1 in hand/,
    );
    expect(snap).toEqual(before);
  });

  it("throws when the pick uses a card more often than the hand holds it", () => {
    let snap = game();
    snap = withHand(snap, 1, [ProgramCard.move1(), ProgramCard.left()]);

    expect(() =>
      submitProgram(snap, 1, prog(ProgramCard.move1(), ProgramCard.move1())),
    ).toThrowError(/Not enough MOVE1 in hand/);
  });

  it("throws when more than five cards are picked", () => {
    let snap = game();
    const six = [
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
      ProgramCard.left(),
    ];
    snap = withHand(snap, 1, six);

    expect(() => submitProgram(snap, 1, prog(...six))).toThrowError(
      /at most 5 cards/,
    );
  });

  it("auto-completes three cards to five and persists the draws", () => {
    let snap = game();
    snap = withHand(snap, 1, [
      ProgramCard.move1(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ]);
    snap = withDrawPile(snap, 1, [
      ProgramCard.left(),
      ProgramCard.right(),
      ProgramCard.uturn(),
    ]);

    const next = submitProgram(
      snap,
      1,
      prog(ProgramCard.move1(), ProgramCard.move1(), ProgramCard.move1()),
    );

    expect(next.players[0].program).toEqual(
      prog(
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.left(),
        ProgramCard.right(),
      ),
    );
    expect(next.players[0].locked).toBe(true);
    // The two drawn cards left the draw pile and joined the hand.
    expect(next.decks["1"].drawPile).toEqual(prog(ProgramCard.uturn()));
    expect(next.decks["1"].hand).toEqual(
      prog(
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.left(),
        ProgramCard.right(),
      ),
    );
  });

  it("an empty pick legally fills all five registers", () => {
    let snap = game();
    snap = withHand(snap, 1, []);
    snap = withDrawPile(snap, 1, [
      ProgramCard.move1(),
      ProgramCard.move2(),
      ProgramCard.left(),
      ProgramCard.right(),
      ProgramCard.uturn(),
      ProgramCard.back1(),
    ]);

    const next = submitProgram(snap, 1, []);

    expect(next.players[0].program).toEqual(
      prog(
        ProgramCard.move1(),
        ProgramCard.move2(),
        ProgramCard.left(),
        ProgramCard.right(),
        ProgramCard.uturn(),
      ),
    );
    expect(next.decks["1"].drawPile).toEqual(prog(ProgramCard.back1()));
  });

  it("a reshuffle triggered by the auto-complete happens once and is persisted", () => {
    const discard = [
      ProgramCard.move1(),
      ProgramCard.move2(),
      ProgramCard.move3(),
      ProgramCard.left(),
      ProgramCard.right(),
    ];
    let snap = game();
    snap = withHand(snap, 1, []);
    snap = withDrawPile(snap, 1, []);
    snap = withDiscardPile(snap, 1, discard);

    const next = submitProgram(snap, 1, []);

    // The whole discard pile was shuffled into the draw pile and dealt out.
    expect(sorted(next.players[0].program!)).toEqual(sorted(prog(...discard)));
    expect(next.decks["1"].drawPile).toEqual([]);
    expect(next.decks["1"].discardPile).toEqual([]);
    expect(sorted(next.decks["1"].hand)).toEqual(sorted(prog(...discard)));
  });

  it("throws for an unknown robot", () => {
    const snap = game();
    expect(() => submitProgram(snap, 7, [])).toThrowError(/No player for robot 7/);
  });

  // Extension for issue #8: a damage card (SPAM/TROJAN_HORSE/WORM) sitting in
  // hand is just another card from validateAndCompleteOrThrow's point of view
  // (issue #14's anti-cheat only checks hand membership by card equality). It
  // is not removed from the deck's hand at submit time; that only happens
  // when the card is actually executed (Game.executeOneRobotTurn's
  // deck.removeFromHand + damageDecks.putBack).
  it("accepts a picked damage card from hand and leaves it in the deck hand until it is executed", () => {
    let snap = game();
    snap = withHand(snap, 1, [
      ProgramCard.spam(),
      ProgramCard.move1(),
      ProgramCard.move1(),
    ]);
    snap = withDrawPile(snap, 1, [
      ProgramCard.left(),
      ProgramCard.right(),
      ProgramCard.uturn(),
    ]);

    const next = submitProgram(
      snap,
      1,
      prog(ProgramCard.spam(), ProgramCard.move1()),
    );

    expect(next.players[0].program).toEqual(
      prog(
        ProgramCard.spam(),
        ProgramCard.move1(),
        ProgramCard.left(),
        ProgramCard.right(),
        ProgramCard.uturn(),
      ),
    );
    expect(next.players[0].locked).toBe(true);
    // SPAM is only removed from the hand when it is executed, not at submit:
    // the original hand is untouched by the picked cards, only grown by the
    // 3 autocompleted draws (left/right/uturn), per Deck.
    // validateAndCompleteOrThrow's javadoc ("autocompleted cards are added to
    // the hand when drawn").
    expect(next.decks["1"].hand).toEqual(
      prog(
        ProgramCard.spam(),
        ProgramCard.move1(),
        ProgramCard.move1(),
        ProgramCard.left(),
        ProgramCard.right(),
        ProgramCard.uturn(),
      ),
    );
    // Damage-card submission never touches the global pools either.
    expect(next.damageDecks).toEqual(snap.damageDecks);
  });

  it("throws when a damage card is picked but not actually present in hand", () => {
    let snap = game();
    snap = withHand(snap, 1, [ProgramCard.move1(), ProgramCard.move1()]);

    expect(() =>
      submitProgram(snap, 1, prog(ProgramCard.trojanHorse())),
    ).toThrowError(/Not enough TROJAN_HORSE in hand/);
  });
});
