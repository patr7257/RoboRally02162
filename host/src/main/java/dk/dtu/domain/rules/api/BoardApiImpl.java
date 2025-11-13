package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.*;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.effects.*;

import java.util.*;

/**
 * Implementation of the BoardAPI interface and provides core game board functionality.
 * <p>
 * BoardAPIImpl also handles multiple board elements (like conveyor belts and antenna) to register planned
 * movements, which are then resolved together with proper priority ordering and collision handling.
 * </p>
 *
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public final class BoardApiImpl implements BoardAPI {
    private final Board board;
    private final Map<Integer, Robot> robots;
    private final List<BeltIntent> intents = new ArrayList<>();
    private List<Integer> priorityOrder = new ArrayList<>();

    public BoardApiImpl(Board board, List<Robot> robots) {
        this.board = board;
        this.robots = new HashMap<>();
        for (Robot robot : robots) {
            this.robots.put(robot.getId(), robot);
        }
    }

    /**
     * Calculates the next coordinate when moving one step in the specified direction.
     *
     * @param from the starting coordinate
     * @param dir the direction of movement
     * @return the coordinate reached after moving one step in the direction
     * @author William Pii Jæger
     */
    public Coord next(Coord from, Direction dir) {
        int nx = switch (dir) {
            case E -> from.x() + 1;
            case W -> from.x() - 1;
            default -> from.x();
        };
        int ny = switch (dir) {
            case S -> from.y() + 1;
            case N -> from.y() - 1;
            default -> from.y();
        };
        return new Coord(nx, ny);
    }

    /**
     * Determines whether a wall exists between two adjacent coordinates.
     * Checks both tiles for walls facing each other. Returns false if the coordinates
     * are not adjacent.
     *
     * @param from the starting coordinate
     * @param to the target coordinate
     * @return {@code true} if a wall blocks movement between the coordinates, {@code false} otherwise
     * @author William Pii Jæger
     */
    public boolean hasWallBetween(Coord from, Coord to) {
        if (!from.isAdjacentTo(to)) {
            return false;
        }

        Direction dir = Direction.fromDelta(from.x(), from.y(), to.x(), to.y());
        if (dir == null) throw new IllegalArgumentException("Not orthogonal neighbors");

        boolean hasWall = false;

        if (this.board.isInBounds(from.x(), from.y())) {
            Tile t1 = board.getTile(from.x(), from.y());
            if (Walls.hasWall(t1, dir)) {
                hasWall = true;
            }
        }
        if (this.board.isInBounds(to.x(), to.y())) {
            Tile t2 = board.getTile(to.x(), to.y());
            if (Walls.hasWall(t2, dir.opposite())) {
                hasWall = true;
            }
        }
        return hasWall;
    }

    /**
     * @author William Pii Jæger
     */
    private Robot robotAt(Coord c) {
        for (Robot r : robots.values()) {
            if (r.getX() == c.x() && r.getY() == c.y()) return r;
        }
        return null;
    }


    /**
     * Attempts to move a robot one step in the specified direction.
     * It checks:
     *
     * <ul>
     *   <li>Wall blocking: Returns Blocked outcome if a wall prevents movement</li>
     *   <li>Falling off board: Creates DestroyEvent if robot moves off the board</li>
     *   <li>Pushing chains: Moves chains of robots if possible, or blocks if immovable</li>
     *   <li>Chain pushed off board: Destroys the tail robot if pushed off the edge</li>
     * </ul>
     *
     * @param robotId the id for the robot
     * @param dir the direction in which to attempt movement
     * @return an Outcome.Moved containing all moves and destroys, or Outcome.Blocked if movement fails
     * @see Outcome.Moved
     * @see Outcome.Blocked
     * @author William Pii Jæger
     */
    @Override
    public Outcome tryMoveOneStep(int robotId, Direction dir) {
        Robot mover = this.robots.get(robotId);

        List<MoveEvent> moves = new ArrayList<>();
        List<DestroyEvent> destroys = new ArrayList<>();

        Coord from = new Coord(mover.getX(), mover.getY());
        Coord adj = next(from, dir);
        if (hasWallBetween(from, adj)) {
            return new Outcome.Blocked(new EdgeBlock(new Edge(from, adj)));
        }

        if (!board.isInBounds(adj.x(), adj.y())) {
            destroys.add(new DestroyEvent(mover.getId(), adj, DestroyCause.FELL_OFF));
            return new Outcome.Moved(List.copyOf(moves), List.copyOf(destroys));
        }

        List<Robot> chain = new ArrayList<>();
        Coord prev = from;
        Coord pos = adj;

        while (true) {
            if (hasWallBetween(prev, pos)) {
                List<Integer> chainIds = chain.stream().map(Robot::getId).toList();
                return new Outcome.Blocked(new RobotChainImmovable(chainIds, new EdgeBlock(new Edge(prev, pos))));
            }

            Robot r = robotAt(pos);
            if (r == null) break;

            chain.add(r);
            prev = pos;
            pos = next(pos, dir);
        }

        boolean tailOffBoard = !board.isInBounds(pos.x(), pos.y());

        if (!chain.isEmpty()) {
            Robot tail = chain.getLast();
            Coord tailFrom = new Coord(tail.getX(), tail.getY());

            if (tailOffBoard) {
                destroys.add(new DestroyEvent(tail.getId(), pos, DestroyCause.FELL_OFF));
            } else {
                moves.add(new MoveEvent(tail.getId(), tailFrom, pos));
            }

            for (int i = chain.size() - 2; i >= 0; i--) {
                Robot r = chain.get(i);
                Coord rFrom = new Coord(r.getX(), r.getY());
                Coord rTo = next(rFrom, dir);
                moves.add(new MoveEvent(r.getId(), rFrom, rTo));
            }
        }

        moves.add(new MoveEvent(mover.getId(), from, adj));

        return new Outcome.Moved(List.copyOf(moves), List.copyOf(destroys));
    }


    /**
     * Adds a movement intent to be resolved later.
     *
     * @param intent about a planned movement
     * @author Weihao Mo
     */
    @Override
    public void addIntent(BeltIntent intent) {
        intents.add(intent);
    }

    /**
     * Resolves all movement intents with priority-based processing.
     * For each priority level:
     * <ul>
     *   <li>Predicts collisions where multiple robots would move to the same location</li>
     *   <li>Processes movements one step at a time for the specified speed</li>
     *   <li>Handles robot chains that are all moving together</li>
     *   <li>Applies conveyor belt rotations after successful movement</li>
     *   <li>Marks robots as having moved to prevent double-activation</li>
     * </ul>
     * <p>
     * After resolution, all intents are cleared from the queue.
     * </p>
     *
     * @return an Outcome.Moved containing all successful moves and any robot destructions
     * @see Outcome.Moved
     * @see Outcome.Blocked
     * @author Weihao Mo
     */
    @Override
    public Outcome resolveIntents() {
        if (intents.isEmpty()) {
            return new Outcome.Moved(List.of(), List.of());
        }

        List<MoveEvent> allMoves = new ArrayList<>();
        List<DestroyEvent> allDestroys = new ArrayList<>();

        Map<Integer, List<BeltIntent>> byPriority = new HashMap<>();
        for (BeltIntent intent : intents) {
            byPriority.computeIfAbsent(intent.priority(), k -> new ArrayList<>()).add(intent);
        }

        List<Integer> priorities = new ArrayList<>(byPriority.keySet());
        priorities.sort(Comparator.reverseOrder());

        for (int priority : priorities) {
            List<BeltIntent> priorityIntents = byPriority.get(priority);
            processPriorityGroup(priorityIntents, priority, allMoves, allDestroys);
        }

        intents.clear();
        return new Outcome.Moved(allMoves, allDestroys);
    }

    /**
     * Processes all intents at a specific priority level.
     * It predicts collisions, processes movements step-by-step for the conveyor speed,
     * and applies rotations after each successful move.
     *
     * @param intents the list of intents at this priority level
     * @param priority the priority level (2 for blue conveyors, 1 for green conveyors)
     * @param allMoves accumulator for all move events
     * @param allDestroys accumulator for all destroy events
     * @author Weihao Mo
     */
    private void processPriorityGroup(List<BeltIntent> intents, int priority,
                                      List<MoveEvent> allMoves, List<DestroyEvent> allDestroys) {
        if (intents.isEmpty()) return;

        int maxSteps = intents.get(0).speed();

        List<Robot> movingRobots = new ArrayList<>();
        for (BeltIntent intent : intents) {
            Robot r = robots.get(intent.robotId());
            if (r != null && r.isAlive() && !r.movedOnActivation()) {
                movingRobots.add(r);
            }
        }

        Set<Integer> blockedByCollision = predictCollisions(movingRobots, maxSteps, priority);

        for (int step = 0; step < maxSteps; step++) {
            List<Robot> robotsToMove = new ArrayList<>();
            for (Robot r : movingRobots) {
                if (r.isAlive()
                        && !blockedByCollision.contains(r.getId())
                        && isRobotOnConveyor(r, priority)) {
                    robotsToMove.add(r);
                }
            }

            if (robotsToMove.isEmpty()) break;

            Map<Integer, Coord> originalPos = new HashMap<>();
            for (Robot r : robotsToMove) {
                originalPos.put(r.getId(), new Coord(r.getX(), r.getY()));
            }

            Map<Robot, Outcome> outcomes = new HashMap<>();
            Set<Integer> movingIds = new HashSet<>();

            for (Robot robot : robotsToMove) {
                movingIds.add(robot.getId());
                Direction dir = getConveyorDirection(robot, priority);
                if (dir != null) {
                    outcomes.put(robot, tryMoveOneStep(robot.getId(), dir));
                }
            }

            Set<Robot> canMove = new HashSet<>();
            for (Robot robot : robotsToMove) {
                Outcome outcome = outcomes.get(robot);
                if (outcome instanceof Outcome.Moved) {
                    canMove.add(robot);
                } else if (outcome instanceof Outcome.Blocked blocked) {
                    if (blocked.reason() instanceof RobotChainImmovable chain) {
                        boolean allMoving = chain.chain().stream().allMatch(movingIds::contains);
                        if (allMoving) {
                            canMove.add(robot);
                        }
                    }
                }
            }

            Set<Integer> processedIds = new HashSet<>();
            List<Robot> movedThisStep = new ArrayList<>();

            for (Robot robot : canMove) {
                if (!robot.isAlive()) continue;

                Outcome outcome = outcomes.get(robot);
                if (outcome instanceof Outcome.Moved moved) {
                    for (DestroyEvent destroy : moved.destroys()) {
                        Robot r = robots.get(destroy.robotId());
                        if (r != null) {
                            r.setDead();
                            processedIds.add(r.getId());
                        }
                        allDestroys.add(destroy);
                    }

                    for (MoveEvent move : moved.moves()) {
                        Robot r = robots.get(move.robotId());
                        if (r != null && !processedIds.contains(r.getId())) {
                            if (r == robot || !movingIds.contains(r.getId())) {
                                r.setPosition(move.to().x(), move.to().y());
                                processedIds.add(r.getId());
                                allMoves.add(move);
                            }
                        }
                    }

                    if (robot.isAlive()) {
                        movedThisStep.add(robot);
                    }
                }
            }

            for (Robot robot : movedThisStep) {
                robot.setMovedOnActivation(true);
            }

            for (Robot robot : movedThisStep) {
                if (robot.isAlive()) {
                    Coord oldPos = originalPos.get(robot.getId());
                    Coord newPos = new Coord(robot.getX(), robot.getY());
                    if (!oldPos.equals(newPos)) {
                        applyRotation(robot, priority);
                    }
                }
            }

            movingRobots = movedThisStep;
        }

        for (int robotId : blockedByCollision) {
            Robot r = robots.get(robotId);
            if (r != null) {
                r.setMovedOnActivation(true);
            }
        }
    }

    /**
     * Predicts which robots will collide during conveyor movement.
     * <p>
     * It simulates the movement of all robots over the specified number of steps without
     * actually moving them. Then we find robots that would attempt to move to the same
     * destination coordinate and mark them as blocked by collision.
     * </p>
     *
     * @param robotList the list of robots to simulate
     * @param maxSteps the number of steps to simulate (conveyor speed)
     * @param priority the priority level to determine which conveyor type to check
     * @return a set of robot IDs that will collide during movement
     * @author Weihao Mo
     */
    private Set<Integer> predictCollisions(List<Robot> robotList, int maxSteps, int priority) {
        Map<Integer, Coord> simPos = new HashMap<>();
        for (Robot r : robotList) {
            simPos.put(r.getId(), new Coord(r.getX(), r.getY()));
        }

        Set<Integer> colliding = new HashSet<>();

        for (int step = 0; step < maxSteps; step++) {
            Map<Coord, List<Integer>> nextDest = new HashMap<>();

            for (Robot robot : robotList) {
                if (colliding.contains(robot.getId())) continue;

                Coord pos = simPos.get(robot.getId());
                if (!isInBounds(pos.x(), pos.y())) continue;

                Direction conveyorDir = getConveyorDirectionAt(pos, priority);
                if (conveyorDir == null) continue;

                Coord nextPos = next(pos, conveyorDir);
                if (hasWallBetween(pos, nextPos)) continue;
                if (!isInBounds(nextPos.x(), nextPos.y())) continue;

                nextDest.computeIfAbsent(nextPos, k -> new ArrayList<>()).add(robot.getId());
            }

            for (var entry : nextDest.entrySet()) {
                if (entry.getValue().size() > 1) {
                    colliding.addAll(entry.getValue());
                }
            }

            for (var entry : nextDest.entrySet()) {
                if (entry.getValue().size() == 1) {
                    simPos.put(entry.getValue().get(0), entry.getKey());
                }
            }
        }

        return colliding;
    }

    /**
     * Gets the conveyor direction at a coordinate
     *
     * @param pos the coordinate
     * @param priority the priority level (2 for blue, 1 for green conveyors)
     * @return the Direction of the conveyor, or {@code null} if no matching conveyor exists
     * @author Weihao Mo
     */
    private Direction getConveyorDirectionAt(Coord pos, int priority) {
        if (!isInBounds(pos.x(), pos.y())) return null;
        Tile tile = getTile(pos.x(), pos.y());
        for (var eff : tile.getEffects()) {
            if (priority == 2 && eff instanceof BlueConveyor bc) return bc.direction();
            if (priority == 1 && eff instanceof GreenConveyor gc) return gc.direction();
        }
        return null;
    }

    /**
     * Checks whether a robot is currently on a conveyor belt of the specified priority.
     *
     * @param robot the robot to check
     * @param priority the priority level
     * @return {@code true} if the robot is on a matching conveyor, {@code false} otherwise
     * @author Weihao Mo
     */
    private boolean isRobotOnConveyor(Robot robot, int priority) {
        if (!isInBounds(robot.getX(), robot.getY())) return false;

        Tile tile = getTile(robot.getX(), robot.getY());
        for (var eff : tile.getEffects()) {
            if (priority == 2 && eff instanceof BlueConveyor) return true;
            if (priority == 1 && eff instanceof GreenConveyor) return true;
        }
        return false;
    }

    /**
     * Gets the direction of the conveyor belt the robot is standing on.
     *
     * @param robot the robot to check
     * @param priority the priority level (2 for blue, 1 for green conveyors)
     * @return the Direction of the conveyor, or {@code null} if no matching conveyor exists
     * @author Weihao Mo
     */
    private Direction getConveyorDirection(Robot robot, int priority) {
        if (!isInBounds(robot.getX(), robot.getY())) return null;

        Tile tile = getTile(robot.getX(), robot.getY());
        for (var eff : tile.getEffects()) {
            if (priority == 2 && eff instanceof BlueConveyor bc) return bc.direction();
            if (priority == 1 && eff instanceof GreenConveyor gc) return gc.direction();
        }
        return null;
    }

    /**
     * Applies conveyor belt rotation to a robot after it has moved
     *
     * @param robot the robot to rotate
     * @param priority the priority level (2 for blue, 1 for green conveyors)
     * @author Weihao Mo
     */
    private void applyRotation(Robot robot, int priority) {
        if (!isInBounds(robot.getX(), robot.getY())) return;

        Tile tile = getTile(robot.getX(), robot.getY());
        for (var eff : tile.getEffects()) {
            Rotation rot = null;
            if (priority == 2 && eff instanceof BlueConveyor bc) {
                rot = bc.rotation();
            }
            if (priority == 1 && eff instanceof GreenConveyor gc) {
                rot = gc.rotation();
            }

            if (rot != null && rot != Rotation.NONE) {
                switch (rot) {
                    case LEFT -> robot.setDirection(robot.getDirection().turnLeft());
                    case RIGHT -> robot.setDirection(robot.getDirection().turnRight());
                }
                return;
            }
        }
    }

    /**
     * Returns all robots currently positioned on the specified tile.
     *
     * @param x the x-coordinate of the tile
     * @param y the y-coordinate of the tile
     * @return a list of robots on the tile, or an empty list if none are present
     * @author William Pii Jæger
     */
    @Override
    public List<Robot> getRobotsOnTile(int x, int y) {
        List<Robot> result = new ArrayList<>();
        for (Robot r : robots.values()) {
            if (r.getX() == x && r.getY() == y) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Retrieves the tile at the specified coordinates.
     *
     * @param x the x-coordinate of the tile
     * @param y the y-coordinate of the tile
     * @return the Tile at position (x, y)
     * @author Weihao Mo
     */
    @Override
    public Tile getTile(int x, int y) {
        return board.getTile(x, y);
    }

    /**
     * @param x the x-coordinate to check
     * @param y the y-coordinate to check
     * @return {@code true} if the coordinates are within bounds, {@code false} otherwise
     * @author William Pii Jæger
     */
    @Override
    public boolean isInBounds(int x, int y) {
        return board.isInBounds(x,y);
    }

    /**
     * @return a list of dead robots, or an empty list if all robots are alive
     *
     * @author Weihao Mo
     */
    public List<Robot> getDeadRobots() {
        List<Robot> result = new ArrayList<>();
        for(Robot r: robots.values()) {
            if(!r.isAlive()) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<Robot> getRobots() {
        return new ArrayList<>(robots.values());
    }

    /**
     * @author Weihao Mo
     */
    @Override
    public void updatePriorityList(List<Integer> priorityOrder) {
        this.priorityOrder = new ArrayList<>(priorityOrder);
    }

    /**
     * @return a list of robots sorted by priority (highest to lowest)
     * @author Weihao Mo
     */
    @Override
    public List<Robot> getRobotsByPriority() {
        List<Robot> sortedRobots = new ArrayList<>();
        for (Integer robotId : priorityOrder) {
            Robot robot = robots.get(robotId);
            if (robot != null) {
                sortedRobots.add(robot);
            }
        }

        for (Robot robot : robots.values()) {
            if (!sortedRobots.contains(robot)) {
                sortedRobots.add(robot);
            }
        }

        return sortedRobots;
    }

}
