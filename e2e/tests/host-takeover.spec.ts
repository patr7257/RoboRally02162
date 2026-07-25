import { test, expect } from "@playwright/test";
import { createGame, joinGame, startGame, e2eGameName } from "./helpers";

/**
 * Issue #13e / mirrors the wave-2 host-handoff smoke (issue #4, #9). Once
 * round 2 programming has started, the host browser context is closed
 * outright (simulating the creator's tab dying). The surviving player is
 * seat 1, so its takeover election fires immediately once it has decided the
 * host is stale (12s with no fresh heartbeat, see client/src/utils/rrr/
 * takeover.ts STALE_MS). After promotion it re-stamps the programming
 * deadline and the 30s timer runs again exactly as it did under the
 * original host, so the round should activate again without any human
 * driving it.
 */
test("closing the host tab mid-game does not stall the player's game", async ({
  browser,
}) => {
  const hostCtx = await browser.newContext();
  const playerCtx = await browser.newContext();
  const host = await hostCtx.newPage();
  const player = await playerCtx.newPage();

  const code = await createGame(host, { gameName: e2eGameName("takeover") });
  expect(code.length).toBeGreaterThanOrEqual(4);

  await joinGame(player, code);
  await startGame(host);

  await host.waitForURL(/#\/boardScene/, { timeout: 20000 });
  await player.waitForURL(/#\/boardScene/, { timeout: 20000 });

  // Round 1: let the timer expire and the round activate on both tabs.
  await expect(host.locator(".last-move-action").first()).toBeVisible({
    timeout: 60000,
  });
  await expect(player.locator(".last-move-action").first()).toBeVisible({
    timeout: 60000,
  });

  // Round 2 programming has started (registers reset). Capture the current
  // move log on the player tab as a baseline fingerprint.
  await expect(player.getByTestId("lock-registers")).toHaveText(
    "LOCK REGISTERS",
    { timeout: 30000 },
  );
  const baseline = await player.locator(".last-move-action").allTextContents();

  // Kill the host tab outright (mid-round-2-programming).
  await hostCtx.close();

  // Within 90s the player tab must notice the stale host, win the election
  // (it is the only remaining seat), re-arm the programming timer, let it
  // expire, and activate round 2 on its own: the move log changes again.
  await expect
    .poll(
      async () => {
        const current = await player
          .locator(".last-move-action")
          .allTextContents();
        return JSON.stringify(current) !== JSON.stringify(baseline);
      },
      { timeout: 90000, intervals: [2000] },
    )
    .toBe(true);

  await playerCtx.close();
});
