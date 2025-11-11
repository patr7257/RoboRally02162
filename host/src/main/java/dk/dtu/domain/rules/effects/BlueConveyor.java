package dk.dtu.domain.rules.effects;

import java.util.*;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Rotation;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.BeltIntent;
/**
 * @author Weihao Mo
 */
public record BlueConveyor(Direction direction, Rotation rotation) implements TileEffect {
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        if (phase != Phase.ACTIVATE_BLUECONVEYOR) return;

        var robots = api.getRobotsOnTile(tile.getX(), tile.getY());
        for (var r : robots) {
            if (!r.isAlive() || r.movedOnActivation()) continue;

            var from = new Coord(r.getX(), r.getY());
            var to = api.next(from, direction);
            int speed = 2;
            int priority = 2;

            api.addIntent(new BeltIntent(r.getId(), from, to, speed, priority, rotation));
        }
    }

    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_BLUECONVEYOR);
    }
}