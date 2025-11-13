package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.Coord;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.api.BoardApiImpl;

import java.util.EnumSet;
/**
 * Wall tile effect that blocks robot movement between adjacent tiles.
 * <p>
 * Each wall is defined by an {@link EnumSet} of {@link Direction directions}
 * corresponding to the edges of the tile that contain walls.
 * </p>
 *
 * <p>
 * It uses logic from BoardApiImpl to determine whether movement between two tiles is allowed.
 * </p>
 *
 * @see BoardApiImpl#hasWallBetween(Coord, Coord) 
 * @author William Pii Jæger
 */
public class Walls implements TileEffect {
    final EnumSet<Direction> edges;

    public Walls(EnumSet<Direction> edges) {
        this.edges = edges;
    }

    /**
     * @author William Pii Jæger
     */
    public static boolean hasWall(Tile t, Direction edge) {
        if (t == null) {
            return false;
        }
        for (TileEffect effect: t.getEffects()) {
            if (effect instanceof Walls walls) {
                if (walls.edges.contains(edge)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @author William Pii Jæger
     */
    public EnumSet<Direction> getEdges() {
        return edges;
    }

    /**
     * @author William Pii Jæger
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
    }

    @Override
    public EnumSet<Phase> phases() {
        return null;
    }
}