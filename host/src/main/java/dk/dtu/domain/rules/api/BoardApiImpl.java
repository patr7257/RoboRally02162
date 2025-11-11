package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.effects.Walls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class BoardApiImpl implements BoardAPI {
    private final Board board;
    private final Map<Integer, Robot> robots;
    private List<Integer> priorityOrder = new ArrayList<>();

    public BoardApiImpl(Board board, List<Robot> robots) {
        this.board = board;
        this.robots = new HashMap<>();
        for (Robot robot : robots) {
            this.robots.put(robot.getId(), robot);
        }
    }

    /**
     * @author William Pii Jæger
     */
    private static Coord next(Coord from, Direction dir) {
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
     * @author William Pii Jæger
     */
    private boolean hasWallBetween(Coord from, Coord to) {
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
     * @author William Pii Jæger
     * @author Weihao Mo
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
     * @author Weihao Mo
     */
    @Override
    public List<Robot> getDeadRobots() {
        List<Robot> result = new ArrayList<>();
        for(Robot r: robots.values()) {
            if(!r.isAlive()) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * @author Weihao Mo
     */
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
