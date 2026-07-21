# HANDOVER

Project: RoboRally. It is playable online today inside patrickrobel.dk/arcade, entirely on
Vercel (no dedicated server). This repo holds the game (TypeScript engine + React client) and
the original DTU Java stack; the serverless backend + arcade cabinet live in `patrickrobelweb`.

## 1. Date, branch, PR, CI
- 2026-07-21. Branch: `main` (the TS/Vercel port, PR #2, is MERGED; feature branch deleted).
- Live: Robot Rally Racer plays at `patrickrobel.dk/arcade` (patrickrobelweb PR #77 merged; the
  built React `client/` bundle is synced into the site and iframed).
- CI: `.github/workflows/ci.yml` runs the Java host + gateway JUnit suites via Nix on PRs. The TS
  `engine/` has its own Vitest suite (100 tests). No deploy pipeline (the arcade bundle is synced
  manually with `pnpm sync:roborally` in patrickrobelweb; pushing here does NOT auto-deploy).
- **Cleanup umbrella issue: #18** (entry point). Feature backlog: EPIC #16 + #3-#15. Deploy
  issue #1 is effectively done (live) and can be closed.

## 2. TLDR of session outcome
- This was a research + prep pass (no code changed here). Established the current reality and
  filed the cleanup umbrella #18 so the next session can plan + implement straight away.
- Reality: the serverless TS/Vercel port is LIVE and canonical (decision recorded in
  patrickrobelweb #88: keep it on Vercel, Upstash Redis + SSE, host-authoritative browser tab).
  The original Java three-tier stack (`gateway/` + `host/` + MySQL) is the graded DTU artifact
  but is no longer the playable path and is homeless. Two rules engines coexist: TS `engine/`
  (canonical) and Java `host/` (reference oracle only).
- Compared against the sibling JavaFX Catan (CatanBoardGame): Catan kept the REAL app and put it
  in the browser via JPro because its UI is JavaFX; RoboRally's client was always React, so no
  JPro is needed and the serverless rewrite is the right path. Opposite choices, both correct.

## 3. Prioritized next steps
1. Do the cleanup in #18: remove dead Java-era React lobby scenes + `REACT_APP_*` + the
   abandoned `webclient/` PoC (subsumes #12); fix the `sync-roborally.mjs` `ROBORALLY_CLIENT_SUBDIR`
   default drift; archive + mark the Java stack non-canonical in the README; reconcile #1/#17.
2. Then attack the biggest UX risk from #16: the host-tab single point of failure (game stalls if
   the host leaves) via host handoff (#4) + host reconnect (#3).
3. Then the deferred gameplay UI the engine already supports: respawn direction (#5), reaction
   cards (#6), damage-card play (#8), timer (#9), post-game/rematch (#10), board selection (#15),
   program anti-cheat (#14), tests (#13).

## 4. Verbatim resume commands (PowerShell)
Sync + open this repo on main:
```
cd "C:\Users\pr\repos\3-Studie\RoboRally02162"; git checkout main; git pull
```
Run the TS engine tests + typecheck + build (run from `engine/`, there is no root package.json):
```
cd "C:\Users\pr\repos\3-Studie\RoboRally02162\engine"; pnpm install; pnpm test; pnpm typecheck; pnpm build
```
Run the site locally to play it (brings up the Upstash-backed API + SSE), then open two tabs:
```
cd "C:\Users\pr\repos\1-Personal\patrickrobelweb\website"; pnpm sync:roborally; pnpm build; $env:PORT=3210; pnpm start
```
```
Start-Process "http://localhost:3210/arcade/games/robot-rally-racer/index.html"
```

## 5. Gotchas discovered / still relevant
- Deployed bundle is the React `client/` build (CRA), NOT `webclient/` (an abandoned PoC). But
  `sync-roborally.mjs` defaults `ROBORALLY_CLIENT_SUBDIR` to `webclient`, so the live bundle was
  synced with an override. Fix this in #18 so a plain `pnpm sync:roborally` reproduces the live app.
- Host-authoritative in the BROWSER: the lobby creator's tab runs the engine. If that tab leaves,
  state survives in Redis but nobody advances the game (patrickrobelweb #88 known limitation).
- `client/src/utils/ws.ts` keeps the old WebSocket-era name/surface but transports over SSE +
  `fetch` to same-origin `/api/robot-rally/*`. Some methods are deliberate no-ops (respawn
  direction, reactions) because the UI does not collect them yet, the engine already can.
- TS `engine/` is standalone (own package.json, esbuild, Vitest); run engine commands from
  `engine/`. It is the canonical rules; Java `host/` is the reference oracle.
- Java quirk faithfully ported: a board with ZERO checkpoints auto-wins after register 1. Deck
  deal / damage draw use Math.random; assertions are range/OR tolerant, do not tighten them.
- This repo has an `upstream` remote with 150+ branches; pin `gh pr ... -R patr7257/RoboRally02162`.
- Vercel deploys (patrickrobelweb) need commit author patr7257 (patr7257@gmail.com); discard
  `website/next-env.d.ts` churn before commit.
- The co-dev gate hook needs this session's harness id in `.claude/.codev-ack` (gitignored).

## 6. Open decisions waiting on Patrick
- Java stack: archive + mark non-canonical (recommended), or still pursue #17 (Dokploy) for a
  server-authoritative option? (Only worth it if the host-tab SPOF must be eliminated.)
- After cleanup, which #16 feature to implement first: host handoff/reconnect (robustness) or the
  visible gameplay gaps (damage cards / reactions / post-game)?

## 7. Environment state
- Both repos clean on `main`. No dev servers, no Docker, no worktrees. The stale merged branch
  `feat/vercel-ts-engine` was deleted locally.
- Production: Robot Rally Racer is live at patrickrobel.dk/arcade (Vercel). Nothing here
  auto-deploys on push.

## Next-session prompt (paste to continue)
Open C:\Users\pr\repos\3-Studie\RoboRally02162, read HANDOVER.md, and start from issue #18: plan
and execute the cleanup (dead Java-era React lobby code + webclient PoC removal, sync-script
default fix, archive/mark the Java stack, reconcile issues #1/#17), then pick the first #16
feature (recommend host handoff #4 + reconnect #3 to kill the host-tab single point of failure).
The serverless TS/Vercel port is already live and canonical; the Java stack is a graded reference.
