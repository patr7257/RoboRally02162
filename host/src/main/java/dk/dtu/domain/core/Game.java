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

/**
 * Core game engine for RoboRally.
 * <p>
 * This class owns the board state, robots, decks, and phase index; executes rounds/registers;
 * applies tile effects via {@link BoardAPI}; evaluates win conditions; and notifies observers.
 * </p>
 * <p>
 * Thread-safety: not thread-safe; expected to be used from a single game-loop thread.
 * </p>
 *
 * @author William Pii Jæger
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Benjamin Benyo Endahl Hansen
 * @author Karl Johannes Agerbo
 */
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

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public Game(Board board, BoardAPI api, List<Robot> robots) {
        this.board = board;
        this.api = api;
        this.robots = robots;
        this.winner = null;
        this.robotsMap = new HashMap<>();
        initGame(robots, null);
        this.phaseIndex = buildPhaseIndex(board.getCells());
        dealNewHands();
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public Game(Board board, BoardAPI api, List<Robot> robots, Map<Integer, Deck> decks) {
        this.board = board;
        this.api = api;
        this.robots = robots;
        this.winner = null;
        this.robotsMap = new HashMap<>();
        initGame(robots, decks);
        this.phaseIndex = buildPhaseIndex(board.getCells());
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    private void initGame(List<Robot> robots, Map<Integer, Deck> decks) {
        int playerCounter = 1;

        for (Robot r : robots) {
            this.robotMap.put(new PlayerID(playerCounter), r);
            this.deckMap.put(r.getId(), decks == null ? new Deck() : decks.get(r.getId()));
            playerCounter++;
        }

        for (Robot robot : robots) {
            this.robotsMap.put(robot.getId(), robot);
        }
    }

    /**
     * Builds an index from phase to tiles that have effects in that phase.
     *
     * @param tiles 2D array of tiles from the board
     * @return map of phase to list of tiles that contain effects for that phase
     * @author William Pii Jæger
     * @author Weihao Mo
     */
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

    /**
     * Returns an immutable copy of a robot's current hand.
     *
     * @param robotID the robot ID
     * @return the robot's hand as an immutable list of program cards
     * @author William Pii Jæger
     */
    public List<ProgramCard> getRobotHand(int robotID) {
        return List.copyOf(deckMap.get(robotID).getHand());
    }

    /**
     * @author Benjamin Benyo Endahl Hansen
     * @author Bjarke Søderhamn Petersen
     * @author Karl Johannes Agerbo
     */
    public List<ProgramCard> getRobotDiscard(int robotID) {
        return List.copyOf(deckMap.get(robotID).getDiscardPile());
    }

    /**
     * Submits and validates a player's selected program (fills remaining slots if allowed) and loads it on the robot.
     *
     * @param player the player ID
     * @param picked the list of picked program cards for this round/registers
     * @throws IllegalArgumentException if no robot is associated with the player
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public void submitProgram(PlayerID player, List<ProgramCard> picked) {
        Robot robot = robotMap.get(player);
        if (robot == null) throw new IllegalArgumentException("No robot for player " + player.value());
        Deck deck = deckMap.get(robot.getId());
        List<ProgramCard> program = deck.validateAndCompleteOrThrow(picked);
        robot.loadProgram(program);
    }

    /**
     * Deals a new hand to every robot and notifies observers of a game update.
     *
     * @author William Pii Jæger
     * @author Bjarke Søderhamn Petersen
     * @author Niklas Emil Lysdal
     */
    public void dealNewHands() {
        for (Robot r : robots) {
            deckMap.get(r.getId()).dealHand(9);
        }
        notifyGameUpdate();
    }

    /**
     * Executes a full round: five registers, checking win conditions after each.
     * If a winner is found, the round stops early. At the end, new hands are dealt and observers notified.
     *
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public void startRound() {
        for (int reg = 1; reg <= 5; reg++) {
            executeRegister(reg);
            if (evaluateWinConditions()) break;
        }
        dealNewHands();
        notifyGameUpdate();
    }

    /**
     * Executes a single register inside the ACTIVATION phase and notifies observers afterward.
     * Also re-evaluates win conditions after execution.
     *
     * @param registerIndex the register number (1..5)
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public void executeRegister(int registerIndex) {
        runPhase(Phase.ACTIVATION, this::executeOneRegister);
        evaluateWinConditions();
        notifyGameUpdate();
    }

    /**
     * Evaluates whether any robot has met the win condition (all checkpoints).
     * Declares and notifies the winner once, the first time this becomes true.
     *
     * @return true if a winner was found (and possibly declared); false otherwise
     * @author Weihao Mo
     */
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

    /**
     * Returns the robot for a given player ID.
     *
     * @param playerID the player ID
     * @return the robot, or {@code null} if none is mapped
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public Robot getRobot(PlayerID playerID) {
        return robotMap.get(playerID);
    }

    /**
     * Returns the list of robots in play.
     *
     * @return list of robots
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public List<Robot> getRobots() {
        return robots;
    }

    /**
     * Returns the game board.
     *
     * @return the board
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Runs a phase body and then applies tile effects for all sub-phases.
     *
     * @param phase the main phase being executed
     * @param body  the code to run for this phase before tile effects
     * @author William Pii Jæger
     * @author Weihao Mo
     */
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
     * Executes robot for the current register without applying tile effect.
     * It differs from {@link #executeRegister(int)} in that it does not apply tile effects after movement
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
     * Runs through all phases and applies their associated tile effects,then evaluate win conditions and notify observer
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
            if(sub != Phase.ACTIVATE_ANTENNA) {
                applyTileEffects(sub);
            }
        }
    }

    /**
     * Applies all tile effects associated with a specific phase.
     * <p>
     * This method retrieves all tiles that have effects in the given phase, executes those effects
     * via {@link TileEffect#onPhase(Phase, Tile, BoardAPI)}, and then resolves the resulting
     * movement and destruction intents through the BoardAPI. Robots are updated accordingly:
     * moved robots have their positions updated, and destroyed robots are marked as dead and
     * have their registers cleared.
     * </p>
     *
     * @param phase the phase for which to apply tile effects
     * @see TileEffect#onPhase(Phase, Tile, BoardAPI)
     * @see BoardAPI#resolveIntents()
     * @author Weihao Mo
     * @author William Pii Jæger
     */
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


    /**
     * Counts the number of checkpoints available on the board by scanning the ACTIVATION phase tiles.
     *
     * @param idx phase index mapping
     * @return number of checkpoints present on the board
     * @author Weihao Mo
     */
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

    /**
     * Reboots (revives) dead robots by marking them alive again.
     * Does not change positions or registers beyond {@link Robot#setAlive()}.
     *
     * @author Weihao Mo
     */
    public void rebootRobots() {
        for (Robot r : robots) {
            if (!r.isAlive()) {
                r.setAlive();
            }
        }
    }

    /**
     * Attempts to move a robot one step in a given direction via {@link BoardAPI#tryMoveOneStep(int, Direction)}.
     * Updates robot positions and death states according to the returned events.
     *
     * @param api the board API used to attempt movement
     * @param r   the robot that is attempting to move
     * @param dir the direction of attempted movement
     * @return true if movement occurred; false if blocked or no movement happened
     * @author William Pii Jæger
     */
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

    /**
     * Executes the next operation for each robot in register order.
     * Movement ops attempt stepwise movement (respecting blocks); rotation ops update direction.
     *
     * @author William Pii Jæger
     */
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

    /**
     * Registers a game observer.
     *
     * @param observer the observer to add
     * @author Weihao Mo
     */
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    /**
     * Unregisters a game observer.
     *
     * @param observer the observer to remove
     * @author Weihao Mo
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies observers that a winner has been declared.
     *
     * @param win the winning player ID
     * @author Weihao Mo
     */
    private void notifyWinner(PlayerID win) {
        for (GameObserver obs : observers) {
            obs.onWinnerDeclared(this,win);
        }
    }


    /**
     * Notifies observers that the game state has been updated.
     *
     * @author William Pii Jæger
     * @author Bjarke Søderhamn Petersen
     * @author Niklas Emil Lysdal
     */
    private void notifyGameUpdate() {
        for (GameObserver obs : observers) {
            obs.onGameUpdate(this);
        }
    }

    /**
     * Declares a winner once (no-op if already declared) and notifies observers.
     *
     * @param win the winning player ID
     * @author Weihao Mo
     */
    public void declareWinner(PlayerID win) {
        if (this.winner != null) return;
        this.winner = win;
        notifyWinner(win);
    }

    /**
     * Returns the current winner, if any.
     *
     * @return an {@link Optional} containing the winner if declared; otherwise empty
     * @author Weihao Mo
     */
    public Optional<PlayerID> getWinner() {
        return Optional.ofNullable(winner);
    }


    /**
     @author Bjarke Søderhamn Petersen
     @author Benjamin Benyo Endahl Hansen
     @author Karl Johannes Agerbo
     */
    public Map<Integer, Deck> getDeckMap() {
        return deckMap;
    }
}
