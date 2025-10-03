package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;

import java.util.List;

// Author(s) William Pii Jæger

public final class BoardApiImpl implements BoardAPI {
    private final Board board;
    private final List<Robot> robots;

    public BoardApiImpl(Board board, List<Robot> robots) {
        this.board = board;
        this.robots = robots;
    }

    @Override
    public MoveOutcome tryMove(int fromX, int fromY, Direction dir, int steps) {
        if (steps == 0) return MoveOutcome.movedTo(fromX, fromY);

        int baseDx = switch (dir) {
            case E -> 1;
            case W -> -1;
            default -> 0;
        };
        int baseDy = switch (dir) {
            case S -> 1;
            case N -> -1;
            default -> 0;
        };

        int sign = (steps > 0) ? 1 : -1;
        int stepDx = baseDx * sign;
        int stepDy = baseDy * sign;

        int x = fromX, y = fromY;
        int todo = Math.abs(steps);

        while (todo-- > 0) {
            int nx = x + stepDx;
            int ny = y + stepDy;

            if (nx < 0 || ny < 0 || nx >= board.getWidth() || ny >= board.getHeight()) {
                return MoveOutcome.blocked("oob");
            }
            x = nx; y = ny;
        }

        return MoveOutcome.movedTo(x, y);
    }

    @Override
    public List<Robot> getRobotsOnTile(int x, int y) {
        return robots.stream().filter(r -> r.getX() == x && r.getY() == y).toList();
    }
}
