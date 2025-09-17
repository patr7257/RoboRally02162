package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.Direction;

// Author(s) Weihao Mo, William Pii Jæger

public interface BoardAPI {
    MoveOutcome tryMove(int fromX, int fromY, Direction direction, int steps);
}
