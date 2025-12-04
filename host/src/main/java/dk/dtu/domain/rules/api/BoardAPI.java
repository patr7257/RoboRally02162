package dk.dtu.domain.rules.api;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.*;

import java.util.List;

/**
 * An interface for interacting with the game board and managing robot movements.
 *
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public interface BoardAPI {
    Outcome tryMoveOneStep(int robotId, Direction dir);

    List<Robot> getRobotsOnTile(int x, int y);

    Tile getTile(int x, int y);

    boolean isInBounds(int x, int y);

    boolean hasWallBetween(Coord from, Coord to);

    Coord next(Coord from, Direction dir);

    List<Robot> getDeadRobots();
    void addIntent(BeltIntent intent);
    Outcome resolveIntents();
    List<Robot> getRobots();
    void updatePriorityList(List<Integer> priorityOrder);
    List<Robot> getRobotsByPriority();
    void reportDestroy(int robotId, Coord at, DestroyCause cause);
    void reportDestroy(int robotId,Coord at, DestroyCause cause,int damagePower);
}
