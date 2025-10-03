package dk.dtu.domain.rules.effects;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.EnumSet;

// Author(s) Weihao Mo

public record Checkpoint(int number) implements TileEffect {


    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        if (phase == Phase.ACTIVATION) {
            int x = tile.getX();
            int y = tile.getY();
            for (Robot robot : api.getRobotsOnTile(x, y)) {
                robot.advanceCheckpointIfMatches(number);
            }
        }
    }

    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATION);
    }
}
