# HANDOVER

Project: RoboRally. Playable online at patrickrobel.dk/arcade, entirely on Vercel (no dedicated
server). This repo holds the TS engine + React client (canonical) and the archived DTU Java
stack; the serverless backend + arcade cabinet live in `patrickrobelweb`.

## 1. Date, branch, PR, CI
- 2026-07-24, late evening. Session: "RoboRally full issues fixes" (the whole #16 EPIC batch in
  one session with parallel subagents, per-wave PRs, Patrick merge-authorized on green).
- MERGED this session: RoboRally PR #19 (cleanup #18), #21 (wave 0), #22 (wave 1), #23 (CI
  lockfile fix), #25 (wave 2); PatrickRobelWeb PR #131 (sync defaults #130) and #133 (backend
  batch #132, DEPLOYED to production).
- IN FLIGHT: RoboRally **PR #26 (wave 3: rematch #10, Playwright e2e #13e, damage verification
  #8)**. ts-tests green; unit-tests failed on the KNOWN FLAKY archived-Java test (issue #24,
  `GatewayWsHandlerSnapshotTest`, random-register position assert); a rerun of the failed job
  was started. Merge on green (standing authorization).
- Issues closed this session: #1 #3 #4 #5 #6 #7 #9 #11 #12 #14 #15 #17 #18 #20; #130 #132 in
  PatrickRobelWeb. Open: #8 #10 #13 (close via PR #26 merge), #16 (EPIC, close in finale), #24
  (flaky Java test, backlog).
- Board: NEW GitHub Project #9 "RoboRally dev board" (owner patr7257) tracks everything; IDs
  cached in `.claude/skills/co-development-workflow/references/github-projects-cli.md` and in
  auto-memory.

## 2. TLDR of session state
- Waves 0-2 are MERGED on main: ws.ts split into `client/src/utils/rrr/{transport,store,emit,
  hostLoop,takeover}.ts`; engine pausable activation (reactions SANDBOX/WEASEL/SPEED pause
  mid-activation, serialize-at-pause; SPEED auto-resolves), between-rounds respawn phase, frame
  labels, submitProgram hand validation; boardLoader orientation fix (burnout/fractionation);
  board picker (4 boards); late-join block; host resume (hostPrivate, /view-redacted); host
  handoff (heartbeat route + staggered election + resume-route takeover + demotion); optional
  programming timer (Off default) with skew-corrected deadlineAt; structured lastMoves;
  rejection surfacing; CI ts-tests job.
- Wave 3 (PR #26) adds: in-place rematch with monotonic wire rounds (roundBase/matchId),
  PostGamePanel; committed `e2e/` Playwright suite (2 specs: timer-driven full round with
  identical cross-tab positions; host-kill takeover survival), data-* attributes; +19 engine
  damage specs proving oracle conformance (147 engine tests total; client 41; data-layer script
  30 live checks).
- Session-found bugs fixed along the way: board id vs displayName in the envelope (banner 404),
  server-clock skew pinning serverNow() to the last write (deadlines never expired), client
  package-lock invalid yaml entry (CI npm 10), missing jest pretest engine-sync hook.
- NOTHING IS DEPLOYED YET: the live site still runs the pre-batch bundle. Deploy is the finale.

## 3. Prioritized next steps (the finale, then the retrospective)
1. Merge PR #26 once the unit-tests rerun is green (it is the #24 flake; rerun already started).
   Cards #8 #10 #13 to Done on board 9 (item ids in the skill cookbook / via item-list).
2. Close EPIC #16 with a summary comment; card to Done.
3. Cleanup: `git worktree remove --force` + delete branches for `../RoboRally02162-10`, `-13e`,
   `-8`; `git worktree prune`; sync main.
4. DEPLOY: on updated main, `cd client && npm ci && npm run build`; then in a patrickrobelweb
   worktree off latest origin/main run `node website/scripts/sync-roborally.mjs`, commit the
   refreshed `website/public/arcade/games/robot-rally-racer/` (author MUST be patr7257
   <patr7257@gmail.com> for Vercel), draft PR, CI/preview green, merge (authorized), then live
   smoke at patrickrobel.dk/arcade (create a 2-tab game, timer round).
5. Post-deploy cleanup: kill any node on :3210; `git worktree remove --force
   ../patrickrobelweb-smoke` (+ delete its smoke/wave1 branch); refresh this HANDOVER.
6. Task #6 (Patrick's explicit request): agent-usage retrospective for this whole session
   (agent types, model tiers, prompt depth, context passing, parallelism; what research tasks
   should look like) cross-checked against `https://www.prepgenaicerts.com/courses/
   claude-certified-architect-foundations/agentic-loops`, `.../multi-agent-orchestration`,
   `.../subagent-invocation-context-passing` and the rest of CCA-F Domain 1; deliverable: a
   critique + a user-level agentic-patterns skill draft in `~/.claude/skills/`.

## 4. Verbatim resume commands (PowerShell)
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162"; git checkout main; git pull; gh pr checks 26 -R patr7257/RoboRally02162
```
Full verification suite (engine 147, client 41, e2e 2):
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162\engine"; pnpm install; pnpm test; pnpm typecheck; pnpm build
```
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162\client"; npm ci; $env:CI="true"; npm test -- --watchAll=false; npm run build
```
Local stack + e2e (server from the smoke worktree; .env.local already copied there):
```
cd "C:\Users\pr\repos\1-Personal\patrickrobelweb-smoke\website"; node scripts/sync-roborally.mjs; $env:PORT=3210; pnpm start
```
```
cd "C:\Users\pr\repos\1-Personal\RoboRally02162\e2e"; npm ci; npx playwright test
```

## 5. Gotchas discovered / still relevant
- `gh pr checks --watch` and `gh run watch ... | tail` MASK failures (pipe exit codes) and can
  watch the stale draft-skipped run: always `gh run list --commit <sha>` for the ready-triggered
  run, watch THAT, then check job conclusions explicitly before merging.
- CI jobs skip on draft PRs (`if: draft == false` + ready_for_review trigger).
- Archived Java `GatewayWsHandlerSnapshotTest` is flaky (#24): rerun, do not chase.
- The MAIN patrickrobelweb checkout fails `pnpm build` with a Turbopack workspace-root error;
  WORKTREES build fine (cause unknown, environment-specific). Use a worktree for any build.
- Playwright: framer-motion HTML5 drag does NOT respond to synthesized DragEvents; the e2e
  drives rounds via the 30s timer instead. Chromium background-tab throttling stalls the host
  loop in tests: launch args in `e2e/playwright.config.ts` disable it.
- Engine schema changes must be hand-mirrored in `client/src/engine/roborally-engine.d.ts` and
  the bundle re-synced (client pre{start,build,test} hooks run `scripts/sync-engine.mjs`).
- Wire rounds: ALL intent traffic uses `roundBase + snap.round` (rematch safety); per-round
  intent keys on the backend are never deleted (24h TTL).
- Untracked `e2e/` leftovers (node_modules) sit in the main checkout while on main (the dir is
  tracked only on the wave-3 branch until #26 merges). Harmless.
- `.claude/.codev-ack` gate: append `<session_id> use|skip` per session, never rewrite.
- Vercel deploy commits in patrickrobelweb need author patr7257 <patr7257@gmail.com>; discard
  `website/next-env.d.ts` churn; never pipe secrets into CLIs.

## 6. Open decisions waiting on Patrick
- None blocking. Standing authorization exists for: merging green wave PRs + the final deploy
  PR (given 2026-07-24, this session). Anything beyond the finale scope needs a fresh ask.

## 7. Environment state
- RoboRally main checkout: on `main`, clean except untracked `e2e/` leftovers; wave-3 worktrees
  `../RoboRally02162-{10,13e,8}` still exist (remove after #26 merges). patrickrobelweb main
  checkout: clean, on `main` (bundle churn from smokes was confined to `patrickrobelweb-smoke`).
- `patrickrobelweb-smoke` worktree serves :3210 when started; a node server MAY still be
  listening on :3210 (kill before restarting; verify the process path first).
- Production: patrickrobelweb #133 backend IS live; the game bundle is NOT yet redeployed
  (finale step 4).
- An in-session one-shot cron (00:21) exists to resume automatically if this session survives.

## Next-session prompt (paste to continue)
Open C:\Users\pr\repos\1-Personal\RoboRally02162, read HANDOVER.md. Continue the #16 batch
finale: merge PR #26 when its unit-tests rerun is green (known flake #24), close EPIC #16,
cards to Done on board 9, delete wave-3 worktrees, rebuild client on main, sync + commit the
bundle in a patrickrobelweb worktree (author patr7257), draft PR + merge + live smoke at
patrickrobel.dk/arcade, cleanup ports/worktrees, refresh HANDOVER. Then do the agent-usage
retrospective + agentic-patterns skill (task #6 / HANDOVER section 3.6, prepgenaicerts CCA-F
Domain 1). Standing merge authorization from 2026-07-24 covers green wave PRs + the deploy PR.
