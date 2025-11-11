package dk.dtu.domain.core;

import dk.dtu.domain.model.*;
import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;
import dk.dtu.domain.rules.DestroyEvent;
import dk.dtu.domain.rules.MoveEvent;
import dk.dtu.domain.rules.Outcome;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.effects.*;

import java.util.*;

public class Game {
    private final Board board;
    private final BoardAPI api;
    private final Map<Phase, List<Tile>> phaseIndex;
    private final List<Robot> robots;
    private final Map<Integer, Robot> robotsMap;
    private final Map<PlayerID, Robot> robotMap = new HashMap<>();
    private final Map<Integer, Deck> deckMap = new HashMap<>();

    private PlayerID winner;
    private final List<GameObserver> observers = new ArrayList<>();

    // Author(s): William Pii Jæger, Weihao Mo
    public Game(Board board, BoardAPI api, List<Robot> robots) {
        this.board = board;
        this.api = api;
        this.robots = robots;
        this.winner = null;

        int playerCounter = 1;
        for (Robot r : robots) {
            this.robotMap.put(new PlayerID(playerCounter), r);
            this.deckMap.put(r.getId(), new Deck());
            playerCounter++;
        }
        this.robotsMap = new HashMap<>();
        for (Robot robot : robots) {
            this.robotsMap.put(robot.getId(), robot);
        }
        this.phaseIndex = buildPhaseIndex(board.getCells());
        dealNewHands();
    }

    // Author(s): William Pii Jæger, Weihao Mo
    private Map<Phase, List<Tile>> buildPhaseIndex(Tile[][] tiles) {
        final Map<Phase, LinkedHashSet<Tile>> temp = new EnumMap<>(Phase.class);
        for (Phase p : Phase.values()) {
            temp.put(p, new LinkedHashSet<>());
        }

        if (tiles != null) {
            for (Tile[] row : tiles) {
                if (row == null) continue;
                for (Tile tile : row) {
                    if (tile == null) continue;
                    for (TileEffect e : tile.getEffects()) {
                        var phases = e.phases();
                        if (phases == null) continue;
                        for (Phase p : phases) {
                            temp.get(p).add(tile);
                        }
                    }
                }
            }
        }

        final Map<Phase, List<Tile>> idx = new EnumMap<>(Phase.class);
        for (Phase p : Phase.values()) {
            final LinkedHashSet<Tile> set = temp.get(p);
            idx.put(p, new ArrayList<>(set));
        }
        return idx;
    }

    // Author(s): William Pii Jæger
    public List<ProgramCard> getRobotHand(int robotID) {
        return List.copyOf(deckMap.get(robotID).getHand());
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public void submitProgram(PlayerID player, List<ProgramCard> picked) {
        Robot robot = robotMap.get(player);
        if (robot == null) throw new IllegalArgumentException("No robot for player " + player.value());
        Deck deck = deckMap.get(robot.getId());
        List<ProgramCard> program = deck.validateAndCompleteOrThrow(picked);
        robot.loadProgram(program);
    }

    // Author(s): William Pii Jæger, Bjarke, Niklas
    public void dealNewHands() {
        for (Robot r : robots) {
            deckMap.get(r.getId()).dealHand(9);
        }
        notifyGameUpdate();
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public void startRound() {
        for (int reg = 1; reg <= 5; reg++) {
            executeRegister(reg);
            if (evaluateWinConditions()) break;
        }
        dealNewHands();
        notifyGameUpdate();
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public void executeRegister(int registerIndex) {
        runPhase(Phase.ACTIVATION, this::executeOneRegister);
        evaluateWinConditions();
        notifyGameUpdate();
    }

    // Author(s): William Pii Jæger, Bjarke, Niklas
    public boolean evaluateWinConditions() {
        int totalCheckpoints = countCheckpoints(phaseIndex);
        for (Map.Entry<PlayerID, Robot> entry : robotMap.entrySet()) {
            Robot r = entry.getValue();
            if (r.hasWon(totalCheckpoints)) {
                declareWinner(entry.getKey());
                return true;
            }
        }
        return false;
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public Robot getRobot(PlayerID playerID) {
        return robotMap.get(playerID);
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public List<Robot> getRobots() {
        return robots;
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public Board getBoard() {
        return board;
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public void runPhase(Phase phase, Runnable body) {
        if (phase == Phase.ACTIVATION) {
            for (Robot r : robots) r.setMovedOnActivation(false);
        }
        body.run();
        for(Phase sub : Phase.values()) {
            if(sub != Phase.ACTIVATE_ANTENNA) {
                applyTileEffects(sub);
            }
        }
    }

    /**
     *
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public void executeRegisterMovesOnly() {
        for (Robot r : robots) r.setMovedOnActivation(false);
        executeOneRegister();
        evaluateWinConditions();
        notifyGameUpdate();
    }

    /**
     *
     * @author William Pii Jæger
     */
    public void applyBoardEffectsAfterRegister() {
        runAllTilePhases();
        evaluateWinConditions();
        notifyGameUpdate();
    }

    /**
     *
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    private void runAllTilePhases() {
        for (Phase sub : Phase.values()) {
            applyTileEffects(sub);
        }
    }

    // Author(s): William Pii Jæger, Weihao Mo
    public void applyTileEffects(Phase phase) {
        List<Tile> tiles = phaseIndex.getOrDefault(phase, List.of());

        for (Tile tile : tiles) {
            for (TileEffect effect : tile.getEffectsForPhase(phase)) {
                effect.onPhase(phase, tile, api);
            }
        }

        Outcome out = api.resolveIntents();

        if (out instanceof Outcome.Moved moved) {
            for (MoveEvent e : moved.moves()) {
                robotsMap.get(e.robotId()).setPosition(e.to().x(), e.to().y());
            }
            for (DestroyEvent d : moved.destroys()) {
                Robot r = robotsMap.get(d.robotId());
                r.clearRegisters();
                r.setDead();
            }
        }
    }

    // Author(s): Weihao Mo
    private int countCheckpoints(Map<Phase, List<Tile>> idx) {
        int count = 0;
        List<Tile> activationTiles = phaseIndex.getOrDefault(Phase.ACTIVATE_CHECKPOINTS, List.of());
        for (Tile tile : activationTiles) {
            for (TileEffect effect : tile.getEffects()) {
                if (effect instanceof Checkpoint) count++;
            }
        }
        return count;
    }

    // Author(s): Weihao Mo
    public void rebootRobots() {
        for(Robot r: robots) {
            if(!r.isAlive()) {
                r.setAlive();
            }
        }
    }

    // Author(s): William Pii Jæger
    private boolean applyOneStep(BoardAPI api, Robot r, Direction dir) {
        Outcome out = api.tryMoveOneStep(r.getId(), dir);
        if (out instanceof Outcome.Moved moved) {
            for (MoveEvent e : moved.moves()) {
                robotsMap.get(e.robotId()).setPosition(e.to().x(), e.to().y());
            }
            for (DestroyEvent d : moved.destroys()) {
                robotsMap.get(d.robotId()).clearRegisters();
                robotsMap.get(d.robotId()).setDead();
            }
            return true;
        }
        if (out instanceof Outcome.Blocked) {
            return false;
        }
        return false;
    }

    // Author(s): William Pii Jæger
    private void executeOneRegister() {
        for (Robot r : api.getRobotsByPriority()) {
            ProgramOP op = r.pollNextOp();
            if (op == null) continue;

            if (op instanceof ProgramOP.Move(int stepsVal)) {
                Direction dir = r.getDirection();
                int steps = stepsVal;
                if (steps < 0) {
                    dir = dir.opposite();
                    steps = -steps;
                }
                while (steps-- > 0) {
                    boolean ok = applyOneStep(api, r, dir);
                    if (!ok) break;
                }
            } else {
                r.setDirection(op.apply(r.getDirection()));
            }
        }
    }

    // Author(s): Weihao Mo
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    // Author(s): Weihao Mo
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    // Author(s): Weihao Mo
    private void notifyWinner(PlayerID win) {
        for (GameObserver obs : observers) {
            obs.onWinnerDeclared(win);
        }
    }

    // Author(s): William Pii Jæger, Bjarke, Niklas
    private void notifyGameUpdate() {
        for (GameObserver obs : observers) {
            obs.onGameUpdate(this);
        }
    }

    // Author(s): Weihao Mo
    public void declareWinner(PlayerID win) {
        if (this.winner != null) return;
        this.winner = win;
        notifyWinner(win);
    }

    // Author(s): Weihao Mo
    public Optional<PlayerID> getWinner() {
        return Optional.ofNullable(winner);
    }
}
