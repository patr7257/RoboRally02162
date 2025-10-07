package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

public class Walls implements TileEffect {
    final EnumSet<Direction> edges;

    public Walls(EnumSet<Direction> edges) {
        this.edges = edges;
    }

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

    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {

    }

    @Override
    public EnumSet<Phase> phases() {
        return null;
    }
}