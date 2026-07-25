# HANDOVER

Project: RoboRally. Playable online at patrickrobel.dk/arcade, entirely on Vercel (no dedicated
server). This repo holds the TS engine + React client (canonical) and the archived DTU Java
stack; the serverless backend + arcade cabinet live in `patrickrobelweb`.

## 1. Date, branch, PR, CI
- 2026-07-25, 00:35 (auto-resumed session). Branch: `main`, clean, synced.
- THE #16 EPIC IS COMPLETE AND DEPLOYED. Merged this batch: RoboRally PRs #19 #21 #22 #23 #25
  #26; PatrickRobelWeb PRs #131 #133 #135. Live bundle `main.e0b7c456.js` verified on
  production (index served + API round-trip green).
- All issues closed except #24 (flaky archived-Java gateway test, documented backlog). Board
  #9: everything on Done.
- CI: `ci.yml` runs Java suites + the new `ts-tests` job (engine vitest/typecheck/build, client
  jest + build) on non-draft PRs. The committed e2e stays manual (needs local stack).

## 2. TLDR of what shipped (Phase 4)
Host handoff/takeover (heartbeat + staggered election + resume-route fencing), host refresh
resume (hostPrivate, /view-redacted), optional programming timer with skew-corrected deadlines
and auto-submit, respawn direction prompts (between-rounds phase), reaction cards
SANDBOX/WEASEL/SPEED (mid-activation pause, serialize-at-pause snapshot cursor; SPEED
auto-resolves), per-register move log from labeled frames, damage cards verified line-for-line
against the Java oracle, program anti-cheat validation, board selection (Starter-Course,
Burnout, Death-Trap, Fractionation; boardLoader orientation fix), late-join block, in-place
rematch with monotonic wire rounds. Architecture: ws.ts split into
`client/src/utils/rrr/{transport,store,emit,hostLoop,takeover}.ts`. Tests: 147 engine, 41
client, 2 Playwright e2e (committed under `e2e/`, run locally), 30-check data-layer script
(`scripts/data-layer-round.mjs`).

## 3. Prioritized next steps
1. Nothing urgent. Optional backlog: #24 (make the flaky Java gateway assertion tolerant or pin
   its deck), spectator mode (declined-for-now half of #11), CI e2e if a mock Redis ever lands.
2. In progress in-session (not repo work): Patrick's agent-usage retrospective + a user-level
   agentic-patterns skill (see auto-memory `phase4-batch-session-state`).

## 4. Verbatim resume commands (PowerShell)
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162"; git checkout main; git pull
```
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162\engine"; pnpm install; pnpm test; pnpm typecheck; pnpm build
```
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162\client"; npm ci; $env:CI="true"; npm test -- --watchAll=false; npm run build
```
Local two-tab stack + e2e (see `e2e/README.md`): build client, then in a patrickrobelweb
WORKTREE (main checkout has an environment-specific Turbopack build failure) run
`node website/scripts/sync-roborally.mjs; pnpm build; $env:PORT=3210; pnpm start`, then
`cd RoboRally02162\e2e; npm ci; npx playwright test`.

## 5. Gotchas still relevant
- `gh pr checks --watch | tail` masks failures and can watch a stale draft-skipped run: resolve
  the run by commit SHA (`gh run list --commit`), watch it, check job conclusions explicitly.
- #24: `GatewayWsHandlerSnapshotTest` is flaky (random auto-completed registers); rerun, don't chase.
- patrickrobelweb MAIN checkout fails `pnpm build` (Turbopack root inference); worktrees build fine.
- framer-motion HTML5 drag ignores synthesized DragEvents; e2e drives rounds via the 30s timer.
  Chromium background-tab throttling stalls the host loop in tests (launch args in
  `e2e/playwright.config.ts` handle it).
- Engine schema changes must be mirrored by hand in `client/src/engine/roborally-engine.d.ts`;
  client pre{start,build,test} hooks copy the engine bundle.
- All intent traffic uses wire rounds (`roundBase + snap.round`); backend intent keys are
  per-round, 24h TTL, never deleted.
- Vercel deploy commits in patrickrobelweb need author patr7257 <patr7257@gmail.com>.
- Board #9 IDs + move commands: `.claude/skills/co-development-workflow/references/github-projects-cli.md`.

## 6. Open decisions waiting on Patrick
- None. The 2026-07-24 standing merge authorization is CONSUMED (batch finished); future pushes
  and merges need fresh per-turn go-aheads.

## 7. Environment state
- Both repos on clean, synced `main`. No worktrees beyond the primaries, no dev servers, port
  3210 free, no Docker. Production live and smoke-tested.

## Next-session prompt (paste to continue)
Open C:\Users\pr\repos\1-Personal\RoboRally02162 and read HANDOVER.md. The Phase 4 EPIC (#16)
is complete and live; there is no in-flight work. Pick from section 3 (e.g. fix flaky #24) or
start something new via the co-development-workflow skill (board #9 tracks all work).
