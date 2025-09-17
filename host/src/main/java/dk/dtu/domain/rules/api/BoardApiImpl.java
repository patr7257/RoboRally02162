package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;

// Author(s) William Pii Jæger

public final class BoardApiImpl implements BoardAPI {
    private final Board board;

    public BoardApiImpl(Board board) {
        this.board = board;
    }

    @Override
    public MoveOutcome tryMove(int fromX, int fromY, Direction dir, int steps) {
        int dx = switch (dir) {
            case E -> 1;
            case W -> -1;
            default -> 0;
        };
        int dy = switch (dir) {
            case S -> 1;
            case N -> -1;
            default -> 0;
        };
        int nx = fromX + dx, ny = fromY + dy;

        if (nx < 0 || ny < 0 || nx >= board.getWidth() || ny >= board.getHeight()) {
            return MoveOutcome.blocked("oob");
        }

        return MoveOutcome.movedTo(nx, ny);
    }
}
