package dk.dtu.domain.rules.api;

import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.Outcome;

import java.util.List;

// Author(s) Weihao Mo, William Pii Jæger

public interface BoardAPI {
    Outcome tryMoveOneStep(int robotId, Direction dir);
    List<Robot> getRobotsOnTile(int x, int y);
    List<Robot> getDeadRobots();
}
