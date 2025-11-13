package dk.dtu.domain.rules.effects;


import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

/**
 * A tile effect that can activate during specific game phases.
 * <p>
 * Each {@code TileEffect} defines one or more {@link Phase phases} during which
 * it triggers and an {@link #onPhase(Phase, Tile, BoardAPI)} method that applies
 * its behavior to the tile and any robots or elements on it.
 * </p>
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public interface TileEffect {
    void onPhase(Phase phase, Tile tile, BoardAPI api);
    EnumSet<Phase> phases();

    default boolean triggersOn(Phase p) {
        return phases().contains(p);
    }
}
