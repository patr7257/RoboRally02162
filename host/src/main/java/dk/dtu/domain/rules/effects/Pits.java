package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.Coord;
import dk.dtu.domain.rules.DestroyCause;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;
import java.util.List;

/**
 * A pits tile kills any {@link Robot} standing on it
 * @author Weihao Mo
 */
public record Pits() implements TileEffect{
    /**
     * @author Weihao Mo
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        int x = tile.getX();
        int y = tile.getY();
        for(Robot robot: api.getRobotsOnTile(x, y)) {
            api.reportDestroy(robot.getId(), new Coord(x, y), DestroyCause.PITS);
        }
    }

    /**
     * @author Weihao Mo
     */
    public static boolean hasPits(Tile t) {
        if (t == null) {
            return false;
        }
        for (TileEffect effect: t.getEffects()) {
            if (effect instanceof Pits) {
                return true;
            }
        }
        return false;
    }

    /**
     * @author Weihao Mo
     */
    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_PITS);
    }
}
