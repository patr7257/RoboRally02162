package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

/**
 * Reboot token tile effect that restores destroyed robots during the reboot phase.
 * @author Weihao Mo
 */
public record RebootToken(Direction direction) implements TileEffect {

    /**
     * @author Weihao Mo
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
    }

    /**
     * @author Weihao Mo
     */
    @Override
    public EnumSet<Phase> phases() {
        return null;
    }
}
