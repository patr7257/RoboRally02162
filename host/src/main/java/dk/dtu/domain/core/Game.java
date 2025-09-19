package dk.dtu.domain.core;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.MoveOutcome;
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

    public Game(Board board, BoardAPI api, List<Robot> robots) {
        this.board = board;
        this.api = api;
        this.robots = robots;
        int playerCounter = 1;
        for (Robot r : robots) {
            this.robotMap.put(new PlayerID(playerCounter), r);
            playerCounter++;
        }
        this.phaseIndex = buildPhaseIndex(board.getCells());
    }

    private Map<Phase, List<Tile>> buildPhaseIndex(Tile[][] tiles) {
        Map<Phase, List<Tile>> phaseIndex = Collections.emptyMap();

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
        runPhase(Phase.PROGRAM_CARD, this::executeProgramCards);
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
    }

    private void executeProgramCards() {
        for (Robot r : robots) {
            ProgramOP op = r.pollNextOp();
            if (op == null) continue;

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
}
