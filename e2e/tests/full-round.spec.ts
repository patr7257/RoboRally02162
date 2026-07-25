import { test, expect } from "@playwright/test";
import {
  createGame,
  joinGame,
  startGame,
  readRobotPositions,
  e2eGameName,
} from "./helpers";

/**
 * Issue #13e. Two real browser contexts (host + player) play a full round
 * against the local server and are asserted to land on IDENTICAL final
 * positions, without either tab ever dragging a card.
 *
 * Why no dragging: the hand cards are HTML5-draggable framer-motion
 * components whose drop payload is carried entirely by React state set from
 * native `dragstart`/`drop` DOM events. Neither Playwright's mouse-based
 * `dragTo()` nor a synthesized `DragEvent` dispatch triggers those handlers
 * (verified during this slice; see README.md). The reliable, already-proven
 * escape hatch is the 30s programming timer (issue #9): when it expires, the
 * host auto-completes every seat with no pick to an empty program, which the
 * engine legally fills from the top of that robot's deck. Both tabs then
 * replay the SAME activation frames, so asserting identical final positions
 * is valid regardless of which cards were auto-drawn.
 */
test("two tabs play a full round on a 30s timer and land on identical positions", async ({
  browser,
}) => {
  const hostCtx = await browser.newContext();
  const playerCtx = await browser.newContext();
  const host = await hostCtx.newPage();
  const player = await playerCtx.newPage();

  try {
    const code = await createGame(host, { gameName: e2eGameName("full-round") });
    expect(code.length).toBeGreaterThanOrEqual(4);

    await joinGame(player, code);
    await startGame(host);

    await host.waitForURL(/#\/boardScene/, { timeout: 20000 });
    await player.waitForURL(/#\/boardScene/, { timeout: 20000 });

    await expect(host.locator(".programSlotsGrid")).toBeVisible();
    await expect(player.locator(".programSlotsGrid")).toBeVisible();
    await expect(host.getByTestId("lock-registers")).toBeVisible();
    await expect(player.getByTestId("lock-registers")).toBeVisible();

    // (i) Let the timer expire and the round activate: both tabs get a move log.
    await expect(host.locator(".last-move-action").first()).toBeVisible({
      timeout: 60000,
    });
    await expect(player.locator(".last-move-action").first()).toBeVisible({
      timeout: 60000,
    });

    // Let the activation animation fully settle on both tabs before reading
    // positions: the CSS transition on robot-absolute is 0.3s.
    await host.waitForTimeout(1000);
    await player.waitForTimeout(1000);

    // (ii) Both tabs replayed the same frames from the same authoritative
    // snapshot, so every robot must be at the same position on both tabs.
    const hostPositions = await readRobotPositions(host);
    const playerPositions = await readRobotPositions(player);
    expect(Object.keys(hostPositions).length).toBeGreaterThan(0);
    expect(playerPositions).toEqual(hostPositions);

    // (iii) Round 2 programming starts: the lock button is back to its
    // unsubmitted label on both tabs (registers reset for the new hand).
    await expect(host.getByTestId("lock-registers")).toHaveText(
      "LOCK REGISTERS",
      { timeout: 30000 },
    );
    await expect(player.getByTestId("lock-registers")).toHaveText(
      "LOCK REGISTERS",
      { timeout: 30000 },
    );
  } finally {
    await hostCtx.close();
    await playerCtx.close();
  }
});
