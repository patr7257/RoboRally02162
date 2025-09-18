package dk.dtu.domain.rules.effects;


import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

// Author(s) Weihao Mo, William Pii Jæger

public interface TileEffect {
    void onPhase(Phase phase, Tile tile, BoardAPI api);
    EnumSet<Phase> phases();

    default boolean triggersOn(Phase p) {
        return phases().contains(p);
    }
}
