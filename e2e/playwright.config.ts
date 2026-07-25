import { defineConfig, devices } from "@playwright/test";
import * as fs from "node:fs";

// This suite drives the real app (two browser contexts) against a locally
// running copy of the sibling patrickrobelweb site, which serves the built
// Robot Rally Racer bundle same-origin on port 3210. See README.md for the
// full runbook. It is NOT run in CI.

/**
 * Prefer a Playwright-managed Chromium selected via PLAYWRIGHT_EXECUTABLE_PATH.
 * When that env var is unset, fall back to the system-installed Chromium at
 * the fixed path below if it exists on disk, so `npm ci && npx playwright test`
 * works without a separate `npx playwright install` download. When neither is
 * available, Playwright falls back to its own managed browser (requires
 * `npx playwright install chromium` to have been run at least once).
 */
const FALLBACK_CHROME_PATH =
  "C:/Users/pr/AppData/Local/ms-playwright/chromium-1228/chrome-win64/chrome.exe";

const executablePath =
  process.env.PLAYWRIGHT_EXECUTABLE_PATH ||
  (fs.existsSync(FALLBACK_CHROME_PATH) ? FALLBACK_CHROME_PATH : undefined);

/**
 * These specs run two browser contexts (host + player) side by side and rely
 * on the host tab's setInterval-driven loop (client/src/utils/rrr/hostLoop.ts,
 * ticking every 1.5s) to keep advancing the round even though it is not the
 * foreground tab. Chromium throttles timers on backgrounded/occluded pages by
 * default, which otherwise stalls the host loop indefinitely (observed while
 * writing this suite: the countdown reached 0:00 client-side but the round
 * never activated because the host tab's tick had been throttled to a crawl).
 * Disabling backgrounding entirely keeps both tabs running at full speed.
 */
const ANTI_THROTTLE_ARGS = [
  "--disable-background-timer-throttling",
  "--disable-backgrounding-occluded-windows",
  "--disable-renderer-backgrounding",
];

export default defineConfig({
  testDir: "./tests",
  // A single 30s programming timer round plus a second round of programming
  // takes roughly 40-60s end to end; host-takeover waits up to 90s more.
  timeout: 150_000,
  expect: { timeout: 20_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:3210",
    trace: "retain-on-failure",
    actionTimeout: 20_000,
    navigationTimeout: 30_000,
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        launchOptions: {
          ...(executablePath ? { executablePath } : {}),
          args: ANTI_THROTTLE_ARGS,
        },
      },
    },
  ],
});
