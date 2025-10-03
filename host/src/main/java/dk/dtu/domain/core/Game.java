package dk.dtu.domain.core;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.MoveOutcome;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.TileEffect;

import java.util.*;

// Author(s) William Pii Jæger, Weihao Mo

public class Game {
    // The way this is structured, game actually doesn't need the board
    // Keeping it here for now
    private final Board board;
    private final BoardAPI api;
    // We iterate over these tile effects, added it so we don't forget
    private final Map<Phase, List<Tile>> phaseIndex;
    private final List<Robot> robots;
    private final Map<PlayerID, Robot> robotMap = new HashMap<>();

    private PlayerID winner;
    private final List<GameObserver> observers = new ArrayList<>();


    public Game(Board board, BoardAPI api, List<Robot> robots) {
        this.board = board;
        this.api = api;
        this.robots = robots;
        this.winner = null;
        int playerCounter = 1;
        for (Robot r : robots) {
            this.robotMap.put(new PlayerID(playerCounter), r);
            playerCounter++;
        }
        this.phaseIndex = buildPhaseIndex(board.getCells());
    }

    private Map<Phase, List<Tile>> buildPhaseIndex(Tile[][] tiles) {
        Map<Phase, List<Tile>> phaseIndex = new HashMap<>();

        for (Tile[] value : tiles) {
            for (Tile tile : value) {
                if (tile == null) continue;
                for (TileEffect effect : tile.getEffects()) {
                    for (Phase phase : effect.phases()) {
                        phaseIndex.computeIfAbsent(phase, k -> new ArrayList<>()).add(tile);
                    }
                }

            }
        }
        return phaseIndex;
    }

    public void startRound() {
        runPhase(Phase.ACTIVATION, this::executeProgramCards);
        evaluateWinConditions();
    }

    public void evaluateWinConditions() {
        int totalCheckpoints = countCheckpoints(phaseIndex);
        for (Map.Entry<PlayerID, Robot> entry : robotMap.entrySet()) {
            Robot r = entry.getValue();
            if (r.hasWon(totalCheckpoints)) {
                declareWinner(entry.getKey());
            }
        }
    }

    public Robot getRobot(PlayerID playerID) {
        return robotMap.get(playerID);
    }

    public List<Robot> getRobots() {
        return robots;
    }

    public Board getBoard() {
        return board;
    }

    public void runPhase(Phase phase, Runnable body) {
        body.run();
        applyTileEffects(phase);
    }


    private void applyTileEffects(Phase phase) {
        List<Tile> tiles = phaseIndex.getOrDefault(phase, List.of());
        for (Tile tile : tiles) {
            for (TileEffect effect : tile.getEffects()) {
                effect.onPhase(phase, tile, api);
            }
        }
    }

    private int countCheckpoints(Map<Phase, List<Tile>> phaseIndex) {
        int count = 0;
        List<Tile> activationTiles = phaseIndex.getOrDefault(Phase.ACTIVATION, List.of());
        for (Tile tile : activationTiles) {
            for (TileEffect effect : tile.getEffects()) {
                if (effect instanceof Checkpoint) {
                    count++;
                }
            }
        }
        return count;
    }

    private void executeProgramCards() {
        boolean anyOpsLeft;
        do {
            anyOpsLeft = false;
            for (Robot r : robots) {
                ProgramOP op = r.pollNextOp();
                if (op != null) {
                    anyOpsLeft = true;
                    if (op instanceof ProgramOP.Move m) {
                        MoveOutcome out = api.tryMove(r.getX(), r.getY(), r.getDirection(), m.steps());
                        if (out.moved()) {
                            r.setX(out.toX());
                            r.setY(out.toY());
                        }
                    }
                    if (op instanceof ProgramOP.RotateLeft || op instanceof ProgramOP.RotateRight || op instanceof ProgramOP.UTurn) {
                        r.setDirection(op.apply(r.getDirection()));
                    }
                }
            }
        } while (anyOpsLeft);
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyWinner(PlayerID winner) {
        for (GameObserver obs : observers) {
            obs.onWinnerDeclared(winner);
        }
    }

    public void declareWinner(PlayerID winner) {
        if (this.winner != null) return;
        this.winner = winner;
        notifyWinner(winner);
    }

    public Optional<PlayerID> getWinner() {
        return Optional.ofNullable(winner);
    }
}
