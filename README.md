# RoboRally

This repository holds two implementations of the same game, built at different times for
different purposes.

## TypeScript engine + React client (canonical, live)

`engine/` (a standalone TypeScript package: pnpm, Vitest, esbuild) and `client/` (a Create React
App frontend) are the game people actually play. It runs at `patrickrobel.dk/arcade`, hosted
serverless on Vercel: Upstash Redis holds game state, Server Sent Events push updates to the
other tabs, and the lobby creator's browser tab runs the engine (host authoritative). The
serverless backend (the `/api/robot-rally/*` routes and the Vercel deployment itself) lives in
the sibling `patrickrobelweb` repository; this repo only ships the engine and the client bundle
that gets synced into it.

`client/src/utils/ws.ts` keeps the old WebSocket-era name and surface for historical reasons, but
it transports over SSE and `fetch` to a same-origin backend, not a real WebSocket.

### Running it locally

Install and build the engine first, then the client:

```bash
cd engine
pnpm install
pnpm build
pnpm test
pnpm typecheck
```

```bash
cd client
npm install
npm start
```

`npm start` and `npm run build` both run a `prestart`/`prebuild` step that copies the built
engine bundle into `client/src/engine/` so Create React App can bundle it (its ModuleScopePlugin
forbids importing from outside `src/`). To actually play a full game end to end (lobby, Redis
state, SSE), run the site from the sibling `patrickrobelweb` repository, which serves the built
`client/build` bundle behind the real API routes.

## Java three-tier stack (archived, DTU-graded artifact)

`gateway/` and `host/` are the original three-tier Java implementation (Spring Boot gateway,
Spring Boot host, MySQL) built for the DTU 02162 course. It is kept in the repository as the
graded academic artifact and as a reference oracle: the TypeScript engine in `engine/` was
ported from, and is checked against, this Java implementation's game rules. It is **not** the
playable path anymore and receives no new features.

If you need to run it anyway (for grading, or to compare rules behaviour), see the
archived notes and requirements below.

### Requirements
* Java: JDK 23
* Maven: 3.9.9
* Node.js + npm (only needed for the old, now-superseded `client/` wiring against the gateway)

### Running gateway + host locally

#### Without Nix
Install JDK 23 and Maven on your system.

#### With Nix
From each module folder (`gateway` and `host`), run:
```bash
nix-shell -p openjdk23 maven
```

Step 1: start the Gateway
```bash
cd gateway
mvn spring-boot:run
```

Step 2: start the Host
```bash
cd host
mvn spring-boot:run
```

The CI workflow (`.github/workflows/ci.yml`) still runs the Java `host` + `gateway` JUnit suites
on pull requests; this is the only thing CI checks in this repository.

### Deploying the Java stack

The following is preserved from the original deployment notes, for a Linux server hosting the
gateway/host/MySQL stack directly (as opposed to the serverless client above).

Requirements:
- Linux machine
- Java version 23
- Maven version 3.9.9
- npm and npx for the React app
- pm2
- Node.js
- nginx
- sudo permission
- potentially cron for automatic deployment or restarts
- a license to use HTTPS, can be self signed
- MySQL database

The `scripts` folder contains bash scripts for deploying, stopping, checking status, and
restarting the service. Make them executable first:

```bash
chmod +x fileNameHere
```

Allow and deny the following ports:
```
ufw allow 80/tcp
ufw allow 443/tcp
ufw deny 8080/tcp
ufw deny 2948/tcp
```

Clone the repository into `/opt/roborally/app` (create the directory first if it does not
exist):
```bash
mkdir /opt/roborally/app
```

The `scripts` folder also contains an nginx config needed for communication between the gateway
and host. Move it to:
```
/etc/nginx/sites-available/se2-f.compute.dtu.dk
```

Then create a symbolic link:
```bash
sudo ln -s /etc/nginx/sites-available/se2-f.compute.dtu.dk /etc/nginx/sites-enabled/
```

Remove the default nginx page:
```bash
sudo rm /etc/nginx/sites-enabled/default
```

If a URL other than `se2-f.compute.dtu.dk` is used, update it in:
- Gateway: `SecurityConfig.java`, `CorsConfig.java`
- The nginx config
- `./env.production`

The MySQL database should be created with the name `RoboRallyDatabase`, with a MySQL user named
`RoboRallyUser` and password `RoboRallyDatabaseUser`. The gateway creates the necessary tables on
launch using these credentials.

Lastly, restart nginx and run the deploy script.
