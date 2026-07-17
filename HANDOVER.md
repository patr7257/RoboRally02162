# HANDOVER

Project: make RoboRally playable with online multiplayer inside patrickrobel.dk,
entirely on Vercel (no separate server), consolidated with the other arcade games.
This work spans TWO repos: this one (RoboRally02162, the game) and patrickrobelweb
(the website, where the backend + arcade cabinet live).

## 1. Date, branch, PR, CI

- Date: 2026-07-16.
- This repo (RoboRally02162): branch `feat/vercel-ts-engine`, draft PR
  patr7257/RoboRally02162#2. CI `unit-tests` skips (no Java changed). Deploy
  tracking issue: patr7257/RoboRally02162#1.
- Website (patrickrobelweb): branch `feat/robot-rally-racer`, draft PR
  patr7257/patrickrobelweb#77. Vercel preview: pass (green).
- Neither PR is merged. Production is untouched.

## 2. TLDR of session outcome

Phase 1 slice: DONE and verified, deployed to a Vercel preview, not merged.

- Discovered the multiplayer was already fully built, but in Java (Spring gateway +
  Spring host rules engine ~8k LOC + MySQL + WebSockets). Vercel runs no Java and
  no long-lived WebSockets, so that stack cannot go on Vercel as-is.
- Chose to reuse the Music Timeline Quiz pattern: Upstash Redis as a validated
  key-value store, a per-game `rev` counter, and an SSE push channel, with the
  lobby creator's browser as the authoritative engine. No WebSockets, no Java, no
  server timers. Reuses the existing Upstash instance under a `roborally:`
  namespace, so no new infra or cost.
- Built the full backend (patrickrobelweb): `src/lib/roborally-redis.ts` +
  `src/app/api/robot-rally/**` (create/list, host state GET/PUT with version
  optimistic-concurrency, password-gated `view` with cheap version diffing, SSE
  `events`, atomic `HSETNX` seat claim/kick, player `intent`, host `intents`).
- Built a slice browser client `webclient/` in THIS repo (vanilla, same-origin,
  SSE), synced into `public/arcade/games/robot-rally-racer/` by
  `scripts/sync-roborally.mjs` (`pnpm sync:roborally`). Flipped the arcade cabinet
  from showcase to embedded/playable "Robot Rally Racer" with a not-affiliated
  disclaimer (RoboRally is a Renegade/Hasbro trademark; repo + DTU portfolio keep
  the original name).
- Verified: `pnpm build` green; full API flow against real Redis (create, seat,
  PUT version bump, live SSE `{"rev":N}`, view diffing, 409 on stale write, intent
  dedup + token auth); headless two-tab drive synced 2 robots in both tabs via SSE.

Scope reality: the slice is lobby + a live board render only. There is no actual
racing yet (no register execution). That is Phase 2/3.

## 3. Prioritized next steps

1. Phase 2, engine port (the bulk of the remaining work). Port the Java rules
   engine `host/src/main/java/dk/dtu/domain/**` to TypeScript, running in the
   authoritative (lobby-creator) browser. Do it TDD, effect by effect, porting the
   matching JUnit test into Jest FIRST as the oracle, then making it pass. Order:
   movement -> walls -> conveyors (green/blue) -> gears -> board+robot lasers ->
   checkpoints -> pits -> reboots -> damage decks -> phases/reactions. Java tests to
   mirror live in `host/src/test/**` (GameSimulationTest, GameConveyorTest,
   BoardLaserTest, RobotLaserTest, AntennaTest, GameCheckpointTest, GamePitsTest,
   GameRebootTest, GameDamageCardTest, GameManager*Test). Reuse the
   `infrastructure/dto` / `SnapshotMapper` shapes as the TS state schema so
   save/load stays faithful.
2. Phase 3, real gameplay loop. Programming phase: each player POSTs a `program`
   intent (5 register cards); the host polls `intents`, resolves activation when
   all locked or a host-tab timer expires, then PUTs the next state; players watch
   step-by-step activation via SSE. Wire up hands/dealing, priority (antenna),
   round advance, win on final checkpoint. Then port save/load and demo games.
3. Decide the client path: either grow `webclient/` into the full game UI, or port
   the existing React client `client/` (swap `client/src/utils/ws.ts` for an SSE +
   fetch `net/` layer, HashRouter + `homepage=/arcade/games/robot-rally-racer`,
   build to `client/build`) and repoint the sync via
   `ROBORALLY_CLIENT_SUBDIR=client/build`. Recommendation: grow webclient for
   playability first; adopt the richer React board visuals later.
4. Phase 4: host resume/takeover route (like hitster `resume`), reconnection UX,
   seat kick UI, spectator, mobile, and a redacting `view` projection if a mode
   must hide un-revealed programs.
5. Phase 5, go live: only once a full round is playable, mark both PRs ready and
   merge to production, verify on patrickrobel.dk/arcade, update the portfolio
   entry.

## 4. Verbatim resume commands (PowerShell)

Sync latest and open this repo's branch:

    cd "C:\Users\pr\repos\3-Studie\RoboRally02162"; git checkout feat/vercel-ts-engine; git pull

Rebuild the site with the current client bundle and run it locally on port 3210:

    cd "C:\Users\pr\repos\1-Personal\patrickrobelweb\website"; pnpm sync:roborally; pnpm build; $env:PORT=3210; pnpm start

Open the game in a browser to test two tabs (create in one, join in the other):

    Start-Process "http://localhost:3210/arcade/robot-rally-racer"

The two live Vercel-preview URLs (you are logged into Vercel SSO, so these open):

    Start-Process "https://patrickrobelweb-git-feat-robot-rally-racer-przrms-projects.vercel.app/arcade/robot-rally-racer"

## 5. Gotchas discovered this session

- Vercel runs no Java and cannot hold long-lived WebSockets (functions cap at
  5-30 min). The SSE + Redis rev-counter pattern is the way; it is already proven
  in production by the Music Timeline Quiz.
- The host state PUT envelope requires `version`/`createdAt`/`updatedAt`/`gameId`
  present, so the authoritative client must always PUT the full state it read from
  GET `/state` (never a partial object), or it gets 400 invalid state.
- Vercel preview deployments have SSO protection ON, so the preview API cannot be
  curled headlessly (302 to SSO). Test the preview in a browser while logged in;
  test the API headlessly against a local `pnpm start` instead.
- Website Vercel deploys need commit author `patr7257` (patr7257@gmail.com). The
  dev server rewrites `website/next-env.d.ts`; discard it before commit/push.
- This repo has an `upstream` remote with 150+ branches, so always pin
  `gh pr create -R patr7257/RoboRally02162 --head patr7257:<branch>`.
- The co-dev gate hook needs the HARNESS session id (not the claude.ai session id)
  in `.claude/.codev-ack`; `.claude/.codev-ack` is gitignored here now.
- `scripts/sync-roborally.mjs` removes and copies ONLY the robot-rally-racer
  subfolder (never the whole `public/arcade/games/`, unlike sync-minigames).

## 6. Open decisions waiting on Patrick

- Ship the lobby-only slice to production now, or hold the merge until a full
  playable round exists? (Recommendation: hold.)
- Client path for Phase 2+: grow the vanilla `webclient/`, or port the existing
  React `client/`? (Recommendation: grow webclient for playability first.)

## 7. Environment state

- No dev servers running (local next server on 3210 was stopped).
- No Docker, no git worktrees created. Both repos clean; both feature branches
  pushed. Draft PRs open (#2 here, #77 website). Scratchpad test scripts live
  outside both repos and are throwaway.

## Next-session prompt (paste to continue)

Open C:\Users\pr\repos\3-Studie\RoboRally02162, read HANDOVER.md, and continue from
next step 1: begin the Phase 2 TypeScript rules-engine port. Work TDD, one board
effect at a time, porting each matching JUnit test from host/src/test into a Jest
test first (as the oracle) and then making it pass, starting with basic robot
movement. The engine runs in the authoritative lobby-creator browser and mutates
the state blob that gets PUT to /api/robot-rally/games/<id>/state on the Vercel
backend (already built in the patrickrobelweb repo, branch feat/robot-rally-racer,
PR #77). Keep the Java host/ and gateway/ as the reference oracle. Do not merge to
production until a full round is playable.
