# Robot Rally Racer: Playwright two-tab e2e (issue #13e)

A committed, real-browser end-to-end suite that plays a full round of Robot
Rally Racer across two browser contexts (host + player) against a locally
running copy of the game. It is a manual pre-release gate, **not part of CI**
(see "Why this is not in CI" below).

## What it covers

- `tests/full-round.spec.ts`: host creates a game with a 30s programming
  timer, a player joins by code, the host starts. Both tabs land on the board
  scene, the timer expires and the round auto-activates (see "Why no card
  dragging" below), and the test asserts every robot ends up at the
  **identical** position/facing on both tabs (read from the `data-robot-id` /
  `data-x` / `data-y` attributes added to `BoardRenderer` for this slice).
  Round 2 programming then starts on both tabs.
- `tests/host-takeover.spec.ts`: same setup; once round 2 programming has
  started, the host's browser context is closed outright (simulating the
  creator's tab dying). Within 90s the surviving player tab must notice the
  stale host, win the takeover election, and keep the round moving on its
  own (mirrors the wave-2 host-handoff smoke, issues #4 and #9).

## Why no card dragging

The hand cards (`MoveSelector.tsx`) are HTML5-`draggable` `framer-motion`
components. The drop payload rides entirely on React state set inside the
native `dragstart` / `drop` DOM event handlers. Two automation paths were
tried and both failed to trigger those handlers:

1. A synthesized `DragEvent` dispatched via `element.dispatchEvent(...)`.
2. Playwright's built-in mouse-based `locator.dragTo(...)`.

The reliable, already-proven alternative (used by the ad-hoc wave-2 smoke
this suite codifies) is the **programming timer** (issue #9): create the game
with a 30s timer, do not drag anything, and let it expire. The host
auto-completes every seat with no submitted pick to an empty program, which
the engine legally fills from the top of that robot's deck. Both tabs then
replay the exact same activation frames from the same authoritative snapshot,
so asserting identical final positions is valid regardless of which cards
were auto-drawn.

If a future session gets CDP-based dragging
(`Input.setInterceptDrags` + `Input.dispatchDragEvent`, chromium-only) working
reliably, it can replace this without changing the assertions.

## Runbook

All commands are PowerShell, run in the order given.

### 1. Build the game client (this repo)

```
cd C:\Users\pr\repos\1-Personal\RoboRally02162\client
npm run build
```

This also rebuilds `engine/` via the `prebuild` hook (`sync-engine.mjs`). If
you are running this from a worktree (e.g.
`RoboRally02162-13e`), `cd` into that worktree's `client/` instead.

### 2. Sync + build + start the sibling patrickrobelweb site on port 3210

The game is served same-origin by the `patrickrobelweb` site (a separate
repo/worktree, normally a sibling of this repo under
`C:\Users\pr\repos\1-Personal\`). It needs a `website/.env.local` (Upstash
Redis credentials etc.) already in place; this suite does not create one.

```
cd C:\Users\pr\repos\1-Personal\patrickrobelweb\website
pnpm sync:roborally
pnpm build
$env:PORT=3210
pnpm start
```

`pnpm sync:roborally` copies `../../RoboRally02162/client/build` into
`public/arcade/games/robot-rally-racer/` by default. If you built a different
worktree's client in step 1 (e.g. `RoboRally02162-13e`), point the sync at it
instead:

```
$env:ROBORALLY_BUILD_DIR = "C:\Users\pr\repos\1-Personal\RoboRally02162-13e\client\build"
pnpm sync:roborally
Remove-Item Env:\ROBORALLY_BUILD_DIR
```

Leave this server running in its own terminal/window. Verify it responds
before moving on:

```
Invoke-WebRequest http://localhost:3210/arcade/games/robot-rally-racer/index.html -UseBasicParsing | Select-Object -ExpandProperty StatusCode
```

If port 3210 is already occupied by a stale server from an earlier session,
identify the process first (`Get-CimInstance Win32_Process -Filter
"ProcessId = <pid>"` after `netstat -ano | findstr :3210`) and confirm it is
actually a `next start` for `patrickrobelweb...\website` before stopping it.
A build synced from a different (older) worktree will be missing the
`data-*` attributes these specs read, and the specs will fail at the position
assertion rather than at setup, so re-sync + rebuild + restart whenever the
client changes.

### 3. Install and run the e2e suite (this directory)

```
cd C:\Users\pr\repos\1-Personal\RoboRally02162\e2e
npm ci
npx playwright test
```

Useful variants:

```
npx playwright test tests/full-round.spec.ts
npx playwright test --headed
npx playwright show-report
```

### Browser selection

`playwright.config.ts` picks a Chromium executable in this order:

1. `PLAYWRIGHT_EXECUTABLE_PATH`, if set.
2. The system-installed Chromium at
   `C:/Users/pr/AppData/Local/ms-playwright/chromium-1228/chrome-win64/chrome.exe`,
   if that path exists (this is Patrick's machine-wide Playwright browser
   cache; reusing it means `npm ci` does not need to download a browser).
3. Otherwise, Playwright's own managed browser (requires having run
   `npx playwright install chromium` at least once).

Chromium is launched with `--disable-background-timer-throttling
--disable-backgrounding-occluded-windows --disable-renderer-backgrounding`.
Without these flags, whichever of the two tabs Chromium treats as
"backgrounded" gets its timers throttled hard enough that the host's
1.5s activation-loop tick effectively stalls, and the programming timer never
actually triggers an activation even though the on-screen countdown reaches
0:00 (hit while writing this suite).

### Environment variables

- `E2E_BASE_URL` (default `http://localhost:3210`): where the game is served.
- `PLAYWRIGHT_EXECUTABLE_PATH`: see "Browser selection" above.
- `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`: set this before `npm ci` if you are
  relying on the system-installed Chromium fallback, so npm does not also try
  to download Playwright's own managed browser.

## Why this is not in CI

This suite needs a real, running copy of the `patrickrobelweb` site (a
separate repo) backed by a real Upstash Redis instance, on port 3210. GitHub
Actions here has neither that sibling repo checked out nor the Upstash
credentials, and standing that up in CI is out of scope for this slice. Run
it manually before a release as the final gate on top of the engine's Vitest
suite and the client's Jest suite (both of which do run in CI).
