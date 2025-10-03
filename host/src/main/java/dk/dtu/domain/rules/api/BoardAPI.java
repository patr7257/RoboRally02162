package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;

import java.util.List;

// Author(s) Weihao Mo, William Pii Jæger

public interface BoardAPI {
    MoveOutcome tryMove(int fromX, int fromY, Direction direction, int steps);
    List<Robot> getRobotsOnTile(int x, int y);
}
