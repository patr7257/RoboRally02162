# HANDOVER

Project: make RoboRally playable with online multiplayer inside patrickrobel.dk,
entirely on Vercel (no separate server), consolidated with the other arcade games.
This work spans TWO repos: this one (RoboRally02162, the game + the new TypeScript
engine) and patrickrobelweb (the website, where the backend + arcade cabinet live).

## 1. Date, branch, PR, CI

- Date: 2026-07-17.
- This repo (RoboRally02162): branch `feat/vercel-ts-engine`, draft PR
  patr7257/RoboRally02162#2. CI `unit-tests` = skipping (no Java changed; the
  engine is a separate TypeScript package with its own Vitest suite). Deploy
  tracking issue: patr7257/RoboRally02162#1.
- Website (patrickrobelweb): branch `feat/robot-rally-racer`, draft PR
  patr7257/patrickrobelweb#77 (Phase 1 backend + arcade cabinet, untouched this
  session).
- Neither PR is merged. Production is untouched.

## 2. TLDR of session outcome

DONE and verified: the ENTIRE pure rules engine plus the host-authoritative
orchestrator core are ported to TypeScript and green. 100 Vitest tests pass,
`tsc --noEmit` clean, `pnpm build` produces a 57.6kb ESM bundle.

New standalone package `engine/` in this repo (own package.json, strict tsconfig,
Vitest, esbuild). Ported effect by effect, TDD, mirroring each JUnit suite as the
oracle:

- Rules engine: movement (push chains, walls, edge falls), AGAIN, checkpoints,
  green/blue conveyors (curves/collisions/walls), antenna priority, pits, reboot/
  respawn, board + robot lasers, and the full card deck + SPAM/TROJAN/WORM damage
  system with reboot penalties.
- Host orchestrator (`engine/src/host/`): a serializable `GameSnapshot` schema +
  lossless mapper, and `createGame` / `submitProgram` / `allSubmitted` /
  `runActivation` (returns the next snapshot plus per-update animation `frames`
  for SSE). Plus `boardLoader.ts`, which parses the real `webclient/board.json`
  into a snapshot; a full two-robot round runs correctly on the actual 13x10
  Starter-Course.

NOT done (blocked on the live stack, deliberately deferred): the cross-repo
multiplayer wiring. That is the only thing left before a playable round.

## 3. Prioritized next steps

1. Bring up the local stack, then wire + verify the multiplayer loop together.
   Run the website locally (command in section 4) so the Upstash-backed API and
   SSE are live and testable in a browser.
2. Backend (patrickrobelweb, branch feat/robot-rally-racer): add a `program`
   player intent carrying the 5 chosen cards for the round, alongside the existing
   seat/intent routes under `website/src/app/api/robot-rally/games`. Keep the
   host-browser-authoritative model: players POST their program; the host tab
   reads intents, resolves, and PUTs the next state.
3. Webclient (this repo, `webclient/`): load the engine ESM bundle (built to
   `engine/dist/roborally-engine.js`) via a `<script type="module">`, replace the
   lobby-only board render with: a programming UI (show the player's 9-card hand
   from the snapshot deck, pick 5), and a host activation loop that on all-locked
   calls `runActivation`, PUTs the next snapshot, and animates the returned
   `frames`. Reconcile the current webclient state blob (players[{idx,name,color,
   robot:{x,y,dir}}]) with the engine `GameSnapshot` (use the snapshot as the
   authoritative blob; adapt the render).
4. Map the snapshot board to the webclient render (or grow the render to read the
   snapshot board directly). Add between-round respawn UX (dead robots pick a
   respawn direction, then `applyRespawnPhase`; note the engine has the logic, the
   UI does not yet collect the direction).
5. Sync the built bundle into the website (`pnpm sync:roborally`) and verify a full
   two-tab round end to end on `http://localhost:3210/arcade/robot-rally-racer`.
6. Phase 5, go live: only once a full round is playable, mark both PRs ready and
   merge to production, verify on patrickrobel.dk/arcade, update the portfolio.

## 4. Verbatim resume commands (PowerShell)

Sync latest and open this repo's branch:

    cd "C:\Users\pr\repos\3-Studie\RoboRally02162"; git checkout feat/vercel-ts-engine; git pull

Install (first time) and run the full engine test suite + typecheck + build:

    cd "C:\Users\pr\repos\3-Studie\RoboRally02162\engine"; pnpm install; pnpm test; pnpm typecheck; pnpm build

Run the website locally on port 3210 (brings up the Upstash-backed API + SSE):

    cd "C:\Users\pr\repos\1-Personal\patrickrobelweb\website"; pnpm sync:roborally; pnpm build; $env:PORT=3210; pnpm start

Open the game to test two tabs (create in one, join in the other):

    Start-Process "http://localhost:3210/arcade/robot-rally-racer"

## 5. Gotchas discovered this session

- The engine lives in `engine/` and is completely separate from the Java `host/`
  and `gateway/` (kept as the reference oracle). Run engine commands from
  `engine/`, not the repo root. There is no root package.json.
- Faithful Java quirk: a board with ZERO checkpoints auto-wins after register 1
  (nextCheckpoint starts at 1, and hasWon(0) = 1 > 0). Real boards always have a
  checkpoint, so tests that drive multi-register programs must use a board with a
  checkpoint the robot does not reach (see engine/test/host/hostGame.test.ts).
- The deck deal and random damage draw use Math.random; the ported assertions are
  range/OR tolerant exactly like the JUnit ones, so there are no flakes. Do not
  "fix" them into exact-equality checks.
- The card deck / damage system is fully ported now. Programs are loaded onto
  robots directly by the orchestrator (loadProgram) after client-side selection;
  the deck still handles SPAM/TROJAN/WORM play and reboot penalties.
- Dynamic robot lasers are never persisted in the snapshot (added and removed
  within one activation), matching the Java SnapshotMapper.
- `pnpm approve-builds` prompts interactively for esbuild; it is not required,
  esbuild's binary already runs (`pnpm build` works).
- This repo has an `upstream` remote with 150+ branches, so always pin
  `gh pr ... -R patr7257/RoboRally02162 --head patr7257:<branch>`.
- Website Vercel deploys need commit author patr7257 (patr7257@gmail.com); discard
  `website/next-env.d.ts` churn before commit/push.
- The co-dev gate hook needs the HARNESS session id in `.claude/.codev-ack`
  (gitignored here). This session recorded `skip`.

## 6. Open decisions waiting on Patrick

- Client path for the multiplayer UI: grow the vanilla `webclient/` into the full
  programming UI, or port the richer React `client/` (swap ws.ts for SSE+fetch)?
  Recommendation so far: grow webclient for playability first.
- Whether to expose an `activating` status + stream frames one-by-one over SSE, or
  PUT the final snapshot and let clients animate from the frames array attached to
  an event. Recommendation: stream frames as SSE events for smooth animation.

## 7. Environment state

- No dev servers running. No Docker, no git worktrees created.
- Both repos clean; this repo's feature branch pushed. Draft PRs open (#2 here,
  #77 website). Nothing merged. Production untouched.
- Engine `node_modules/` and `dist/` are gitignored; committed files are source,
  tests, config, and pnpm-lock.

## Next-session prompt (paste to continue)

Open C:\Users\pr\repos\3-Studie\RoboRally02162, read HANDOVER.md, and continue from
next step 1: bring up the local stack and wire the Phase 3 multiplayer loop. The
entire TypeScript rules engine and the host-authoritative orchestrator are done and
green in the engine/ package (createGame, submitProgram, allSubmitted, runActivation
returning next snapshot + animation frames; parseBoardDefinition loads the real
board.json). What remains is the cross-repo glue: add a `program` player intent on
the patrickrobelweb backend (branch feat/robot-rally-racer, PR #77), and rewrite
webclient/app.js to load the engine ESM bundle, show a 5-card programming UI from
the snapshot deck, run runActivation on the host tab when all players lock in, PUT
the next snapshot, and animate the frames for players over SSE. Verify a full
two-tab round locally before touching production. Do not merge either PR until a
full round is playable end to end.
