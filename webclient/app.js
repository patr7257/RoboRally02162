/*
  Robot Rally Racer - slice client.

  A small, self-contained browser client that proves the Vercel backend end to
  end: create/join a game, claim a robot seat, and watch the board sync live over
  Server-Sent Events. It talks same-origin to /api/robot-rally/* (the Next.js API
  routes), mirroring how the Music Timeline Quiz client works.

  Model (host-browser-authoritative):
   - The lobby CREATOR's tab is the authority. It holds the hostToken, owns the
     full game state, polls seat claims + intents, and PUTs the next state.
   - Other players CLAIM a seat (getting a playerToken) and only read state
     (GET /view) + submit intents. They appear on the board once the host folds
     their seat into the authoritative state.
   - A per-game rev counter drives an SSE push; on every bump each tab refetches.

  This slice does lobby + live board render only. The turn/programming resolution
  and the full TypeScript rules-engine port land in later phases, reusing this
  same backend unchanged.
*/
(function () {
  "use strict";

  var BASE = "/api/robot-rally";
  var HOST_TOKEN_HEADER = "x-rrr-host-token";
  var MAX_PLAYERS = 6;
  var FALLBACK_POLL_MS = 3000;

  var PALETTE = ["#22d3ee", "#f472b6", "#4ade80", "#fbbf24", "#a78bfa", "#f87171"];
  var DIR_GLYPH = { N: "↑", E: "→", S: "↓", W: "←" };

  // -- DOM helpers -----------------------------------------------------------
  function $(id) { return document.getElementById(id); }
  function show(el) { el.classList.remove("hidden"); }
  function hide(el) { el.classList.add("hidden"); }
  function setText(id, t) { $(id).textContent = t; }

  // -- Board template --------------------------------------------------------
  var BOARD = null;
  var BOARD_W = 0;
  var BOARD_H = 0;
  var EFFECTS = {}; // "x,y" -> [effect, ...]
  var START_TILES = {}; // playerId -> {x, y}
  var START_DIR = "W";

  function loadBoard() {
    return fetch("./board.json")
      .then(function (r) { return r.json(); })
      .then(function (b) {
        BOARD = b;
        BOARD_W = (b.startingBoardWidth || 0) + (b.boardWidth || 0);
        BOARD_H = b.boardHeight || 0;
        START_DIR = (b.startingBoardDirection || "w").toUpperCase();
        var eff = b.effects || {};
        Object.keys(eff).forEach(function (key) {
          if (key.indexOf("_comment") === 0) return;
          EFFECTS[key] = eff[key];
          eff[key].forEach(function (e) {
            if (e.kind === "startingtile" && typeof e.playerId === "number") {
              var parts = key.split(",");
              START_TILES[e.playerId] = { x: Number(parts[0]), y: Number(parts[1]) };
            }
          });
        });
      });
  }

  function startTileForSeat(idx) {
    // Seats are 0-based; starting tiles are keyed by 1-based playerId.
    var tile = START_TILES[idx + 1];
    if (tile) return { x: tile.x, y: tile.y };
    // Fallback: stack along the left edge if a template lacks a seat.
    return { x: 0, y: Math.min(idx, BOARD_H - 1) };
  }

  function makePlayer(idx, name) {
    var st = startTileForSeat(idx);
    return {
      idx: idx,
      name: name && name.length ? name : "Player " + (idx + 1),
      color: PALETTE[idx % PALETTE.length],
      robot: { x: st.x, y: st.y, dir: START_DIR },
    };
  }

  // -- Fetch helpers ---------------------------------------------------------
  function jsonFetch(url, opts) {
    return fetch(url, opts).then(function (res) {
      return res
        .text()
        .then(function (body) {
          var data = null;
          try { data = body ? JSON.parse(body) : null; } catch (e) { data = null; }
          return { ok: res.ok, status: res.status, data: data };
        });
    });
  }

  function deviceId() {
    var id = localStorage.getItem("rrr_device");
    if (!id) {
      id = "d" + Math.random().toString(36).slice(2) + Date.now().toString(36);
      localStorage.setItem("rrr_device", id);
    }
    return id;
  }

  // -- Session state ---------------------------------------------------------
  var session = null; // { gameId, isHost, hostToken?, myIdx, playerToken?, password }
  var localState = null; // authoritative (host) or last-seen (player) game state
  var localVersion = 0;
  var events = null; // EventSource
  var pollTimer = null;
  var busy = false;

  // -- Home actions ----------------------------------------------------------
  function homeError(msg) { setText("home-error", msg || ""); }

  function onCreate() {
    var name = $("create-name").value.trim();
    var gameName = $("create-game-name").value.trim();
    var pw = $("create-pw").value;
    homeError("");
    if (!name) return homeError("Enter your name.");
    if (gameName.length < 1 || gameName.length > 40) return homeError("Game name must be 1 to 40 characters.");
    if (!/^[\x20-\x7e]{4,32}$/.test(pw)) return homeError("Password must be 4 to 32 characters.");

    $("create-btn").disabled = true;
    var initial = {
      name: gameName,
      status: "lobby",
      phase: "lobby",
      round: 0,
      current: 0,
      board: BOARD.displayName,
      players: [makePlayer(0, name)],
    };

    jsonFetch(BASE + "/games", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: gameName, password: pw, state: initial }),
    })
      .then(function (r) {
        if (!r.ok || !r.data) throw new Error((r.data && r.data.error) || "create failed (" + r.status + ")");
        session = {
          gameId: r.data.gameId,
          isHost: true,
          hostToken: r.data.hostToken,
          myIdx: 0,
          password: pw,
        };
        return hostLoadState();
      })
      .then(function () { enterGame(); startHost(); })
      .catch(function (e) { homeError(String(e.message || e)); })
      .finally(function () { $("create-btn").disabled = false; });
  }

  function onJoin() {
    var name = $("join-name").value.trim();
    var code = $("join-code").value.trim().toUpperCase();
    var pw = $("join-pw").value;
    homeError("");
    if (!name) return homeError("Enter your name.");
    if (!/^[A-Z0-9]{4,8}$/.test(code)) return homeError("Enter a valid game code.");
    if (!pw) return homeError("Enter the game password.");

    $("join-btn").disabled = true;
    claimAnySeat(code, pw, name, 1)
      .then(function (res) {
        session = {
          gameId: code,
          isHost: false,
          myIdx: res.idx,
          playerToken: res.playerToken,
          password: pw,
        };
        enterGame();
        startPlayer();
      })
      .catch(function (e) { homeError(String(e.message || e)); })
      .finally(function () { $("join-btn").disabled = false; });
  }

  // Try to claim the lowest free seat starting at `idx`. Resolves { idx, playerToken }.
  function claimAnySeat(code, pw, name, idx) {
    if (idx >= MAX_PLAYERS) return Promise.reject(new Error("game is full"));
    return jsonFetch(BASE + "/games/" + code + "/seats", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pw: pw, playerIdx: idx, deviceId: deviceId(), name: name }),
    }).then(function (r) {
      if (r.ok && r.data && r.data.playerToken) return { idx: idx, playerToken: r.data.playerToken };
      if (r.status === 403) throw new Error("wrong password");
      if (r.status === 404) throw new Error("game not found");
      if (r.status === 409) return claimAnySeat(code, pw, name, idx + 1); // seat taken, try next
      throw new Error((r.data && r.data.error) || "join failed (" + r.status + ")");
    });
  }

  // -- Enter game screen -----------------------------------------------------
  function enterGame() {
    hide($("home"));
    show($("game"));
    setText("game-role", session.isHost ? "Host" : "Player " + (session.myIdx + 1));
    setText("game-code", session.gameId);
    if (session.isHost) {
      setText("game-pw", session.password);
    } else {
      $("game-pw").parentElement.style.display = "none";
    }
    setText(
      "roster-hint",
      session.isHost
        ? "Share the code and password. Players appear as they join."
        : "Waiting for the host. Your robot appears once the host sees you."
    );
    openEvents();
  }

  // -- SSE + fallback poll ---------------------------------------------------
  function openEvents() {
    if (events) events.close();
    events = new EventSource(BASE + "/games/" + session.gameId + "/events");
    events.onopen = function () { setText("game-status", "live"); };
    events.onmessage = function () { refresh(); };
    events.onerror = function () { setText("game-status", "reconnecting"); };
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(refresh, FALLBACK_POLL_MS);
    refresh();
  }

  function refresh() {
    if (busy) return;
    busy = true;
    var p = session.isHost ? hostTick() : playerTick();
    Promise.resolve(p).finally(function () { busy = false; });
  }

  // -- Host authority --------------------------------------------------------
  function hostLoadState() {
    return jsonFetch(BASE + "/games/" + session.gameId + "/state", {
      headers: headerHost(),
    }).then(function (r) {
      if (!r.ok || !r.data) throw new Error("could not load state (" + r.status + ")");
      localState = r.data.state;
      localVersion = r.data.version;
    });
  }

  function headerHost() {
    var h = { "Content-Type": "application/json" };
    h[HOST_TOKEN_HEADER] = session.hostToken;
    return h;
  }

  function startHost() { render(); refresh(); }

  function hostTick() {
    var round = localState ? localState.round : 0;
    return jsonFetch(BASE + "/games/" + session.gameId + "/intents?round=" + round, {
      headers: headerHost(),
    }).then(function (r) {
      if (!r.ok || !r.data) return;
      var seats = r.data.seats || {};
      var known = {};
      localState.players.forEach(function (p) { known[p.idx] = true; });
      var added = false;
      Object.keys(seats).forEach(function (idxStr) {
        var idx = Number(idxStr);
        if (!known[idx]) {
          localState.players.push(makePlayer(idx, seats[idxStr].name));
          added = true;
        }
      });
      if (added) {
        localState.players.sort(function (a, b) { return a.idx - b.idx; });
        return hostPut();
      }
      render();
    });
  }

  function hostPut() {
    return jsonFetch(BASE + "/games/" + session.gameId + "/state", {
      method: "PUT",
      headers: headerHost(),
      body: JSON.stringify({ baseVersion: localVersion, state: localState }),
    }).then(function (r) {
      if (r.ok && r.data && typeof r.data.version === "number") {
        localVersion = r.data.version;
        localState.version = r.data.version;
        render();
      } else if (r.status === 409) {
        // Lost the race; resync and let the next tick reapply.
        return hostLoadState().then(render);
      }
    });
  }

  // -- Player read -----------------------------------------------------------
  function playerTick() {
    var url =
      BASE + "/games/" + session.gameId + "/view?pw=" +
      encodeURIComponent(session.password) + "&v=" + localVersion;
    return jsonFetch(url).then(function (r) {
      if (!r.ok || !r.data) return;
      if (r.data.unchanged) return;
      if (r.data.state) {
        localState = r.data.state;
        localVersion = r.data.version;
        render();
      }
    });
  }

  // -- Render ----------------------------------------------------------------
  function render() {
    renderRoster();
    renderBoard();
  }

  function renderRoster() {
    var list = $("roster-list");
    list.innerHTML = "";
    var players = (localState && localState.players) || [];
    players.forEach(function (p) {
      var li = document.createElement("li");
      var dot = document.createElement("span");
      dot.className = "dot";
      dot.style.background = p.color || PALETTE[p.idx % PALETTE.length];
      var name = document.createElement("span");
      name.textContent = p.name;
      if (p.idx === session.myIdx) name.className = "me";
      li.appendChild(dot);
      li.appendChild(name);
      list.appendChild(li);
    });
    if (!players.length) {
      var li = document.createElement("li");
      li.textContent = "No players yet.";
      list.appendChild(li);
    }
  }

  function cellClasses(effs) {
    var cls = ["cell"];
    if (!effs) return cls;
    effs.forEach(function (e) {
      var kind = String(e.kind || "").toLowerCase();
      if (kind === "startingtile") cls.push("start");
      else if (kind.indexOf("conveyor") !== -1) cls.push("conveyor");
      else if (kind === "gear") cls.push("gear");
      if (kind === "walldto" && Array.isArray(e.walls)) {
        e.walls.forEach(function (w) { cls.push("w-" + String(w).toLowerCase()); });
      }
    });
    return cls;
  }

  function cellMarks(effs) {
    // Returns { corner: string, checkpoint: number|null }
    var corner = "";
    var checkpoint = null;
    (effs || []).forEach(function (e) {
      var kind = String(e.kind || "").toLowerCase();
      if (kind === "checkpoint") checkpoint = e.number;
      else if (kind === "antenna") corner = "A";
      else if (kind === "board_laser") corner = "L";
      else if (kind === "reboot_token") corner = "R";
      else if (kind.indexOf("conveyor") !== -1 && e.direction) corner = DIR_GLYPH[e.direction] || "";
      else if (kind === "gear") corner = e.rotation === "LEFT" ? "↺" : "↻";
    });
    return { corner: corner, checkpoint: checkpoint };
  }

  function renderBoard() {
    var board = $("board");
    board.style.gridTemplateColumns = "repeat(" + BOARD_W + ", 44px)";

    // Index robots by cell.
    var robotsAt = {};
    ((localState && localState.players) || []).forEach(function (p) {
      if (p.robot) robotsAt[p.robot.x + "," + p.robot.y] = p;
    });

    var html = "";
    for (var y = 0; y < BOARD_H; y++) {
      for (var x = 0; x < BOARD_W; x++) {
        var key = x + "," + y;
        var effs = EFFECTS[key];
        var cls = cellClasses(effs).join(" ");
        var marks = cellMarks(effs);
        var inner = "";
        if (marks.checkpoint != null) {
          inner += '<span class="checkpoint">⚑' + marks.checkpoint + "</span>";
        } else if (marks.corner) {
          inner += '<span class="mark">' + marks.corner + "</span>";
        }
        var robot = robotsAt[key];
        if (robot) {
          var glyph = DIR_GLYPH[(robot.robot && robot.robot.dir) || "N"] || "•";
          inner +=
            '<span class="robot" style="background:' + (robot.color || "#fff") + '">' + glyph + "</span>";
        }
        html += '<div class="' + cls + '">' + inner + "</div>";
      }
    }
    board.innerHTML = html;
  }

  // -- Boot ------------------------------------------------------------------
  function boot() {
    $("create-btn").addEventListener("click", onCreate);
    $("join-btn").addEventListener("click", onJoin);
    loadBoard().catch(function () {
      homeError("Could not load the board template.");
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
