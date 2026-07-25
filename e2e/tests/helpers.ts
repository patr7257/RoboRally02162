import { Page, expect } from "@playwright/test";

// Shared lobby + board helpers for the two-tab e2e specs (issue #13e). See
// README.md for the drag-and-drop constraint that shapes these tests: the
// programming timer (30s) drives rounds forward instead of dragging cards.

export const GAME_URL = "/arcade/games/robot-rally-racer/index.html";
export const TIMER_LABEL = "30s";
export const PASSWORD = "e2eSecret1";

/** Game names MUST contain "e2e": the backend gives such games a 1h TTL and
 *  hides them from any public index, so test runs do not pollute the lobby. */
export function e2eGameName(label: string): string {
  return `e2e pw ${label} ${Date.now()}`;
}

async function fillYourName(page: Page, name: string): Promise<void> {
  await page.getByLabel("Your name").fill(name);
}

/** Creates a game as the host: fills the Create-a-game panel, submits, and
 *  returns the generated game code shown in the lobby. */
export async function createGame(
  page: Page,
  opts: { hostName?: string; gameName?: string; timerLabel?: string } = {},
): Promise<string> {
  const hostName = opts.hostName ?? "Host";
  const gameName = opts.gameName ?? e2eGameName("host");
  const timerLabel = opts.timerLabel ?? TIMER_LABEL;

  await page.goto(GAME_URL);
  await fillYourName(page, hostName);

  const panel = page.locator(".control-panel", { hasText: "Create a game" });
  await panel.getByLabel("Game name").fill(gameName);
  await panel.locator('input[type="password"]').fill(PASSWORD);
  await panel.locator("select").selectOption({ label: timerLabel });
  await panel.getByRole("button", { name: "Create", exact: true }).click();

  const code = await page
    .locator('p:has-text("Game code:") strong')
    .innerText();
  return code.trim();
}

/** Joins an existing game as a player via the Join-a-game panel. */
export async function joinGame(
  page: Page,
  code: string,
  opts: { playerName?: string } = {},
): Promise<void> {
  const playerName = opts.playerName ?? "Player";

  await page.goto(GAME_URL);
  await fillYourName(page, playerName);

  const panel = page.locator(".control-panel", { hasText: "Join a game" });
  await panel.getByLabel("Game code").fill(code);
  await panel.locator('input[type="password"]').fill(PASSWORD);
  await panel.getByRole("button", { name: "Join", exact: true }).click();
}

/**
 * Host-only: clicks Start game from the lobby screen. Waits first for the
 * roster `<ul>` to actually list `expectedPlayerCount` seats.
 *
 * This must NOT be a generic getByText(/Player/) wait: the lobby screen's
 * own `<h3>Players</h3>` heading always matches that pattern, even before
 * any player has joined, so such a wait resolves instantly. Clicking Start
 * before the host's lobby-poll loop has merged the joined seat into
 * `state.players` and PUT it back builds the initial engine snapshot with
 * only the host's robot, which then makes activation throw ("No player for
 * robot 2") once the actually-occupied second seat tries to submit (found
 * while writing this suite).
 */
export async function startGame(
  hostPage: Page,
  expectedPlayerCount = 2,
): Promise<void> {
  const roster = hostPage
    .locator(".control-panel", { hasText: "Players" })
    .locator("ul > li");
  await expect(roster).toHaveCount(expectedPlayerCount, { timeout: 20000 });
  await hostPage.getByRole("button", { name: "Start game" }).click();
}

/** Reads every rendered robot's position/aliveness off the board, keyed by
 *  robot id, from the data-* attributes added to BoardRenderer for #13e. */
export async function readRobotPositions(
  page: Page,
): Promise<Record<string, { x: string; y: string; alive: string }>> {
  return page.evaluate(() => {
    const out: Record<string, { x: string; y: string; alive: string }> = {};
    document.querySelectorAll("[data-robot-id]").forEach((el) => {
      const id = el.getAttribute("data-robot-id") ?? "";
      out[id] = {
        x: el.getAttribute("data-x") ?? "",
        y: el.getAttribute("data-y") ?? "",
        alive: el.getAttribute("data-alive") ?? "",
      };
    });
    return out;
  });
}
