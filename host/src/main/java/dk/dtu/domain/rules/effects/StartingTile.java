package dk.dtu.domain.rules.effects;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

/**
 * @author Patrick Røbel
 */
public record StartingTile(int robotId) implements TileEffect {
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
	// probably no effect here?
    }
    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.noneOf(Phase.class);
    }
}