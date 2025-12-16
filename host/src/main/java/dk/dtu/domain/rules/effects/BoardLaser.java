package dk.dtu.domain.rules.effects;

import java.util.*;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.api.BoardAPI;

/**
 * Board Laser tile effect that shoots lasers in the direction they are facing.
 *
 * @param direction the direction the laser is facing
 * @param power the power of the laser (number of SPAM cards dealt: 1, 2, or 3)
 *
 * @author Patrick Røbel
 */
public record BoardLaser(Direction direction, int power) implements TileEffect {
    
    /**
     * When the board laser phase activates, this laser shoots from its tile
     * in its fixed direction, hitting the first robot in line of sight.
     * 
     * @author Patrick Røbel
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        if (phase != Phase.ACTIVATE_BOARD_LASERS) return;

        boolean hasTarget = false;
        
        // Pre calculate laser sight starting from LaserPosition
        Coord currentPos = new Coord(tile.getX(), tile.getY());
        Coord previousPos = currentPos;
        
        // First check if there's a robot on the laser tile itself
        // If so, hit it directly since no walls or antennas can block it
        List<Robot> robotsOnLaser = api.getRobotsOnTile(currentPos.x(), currentPos.y());
        if (!robotsOnLaser.isEmpty()) {
            Robot target = robotsOnLaser.get(0);
            api.reportDestroy(target.getId(), currentPos, DestroyCause.LASER, power);
            api.notifyTileEffectActivated(tile.getX(), tile.getY(), "board_laser");
            return;
        }
        
        // Step through each tile in the laser's direction
        laserPath: while (true) {
            currentPos = api.next(currentPos, direction);
            
            // Check if we've gone off the board
            if (!api.isInBounds(currentPos.x(), currentPos.y())) {
                break;
            }
            
            Tile currentTile = api.getTile(currentPos.x(), currentPos.y());
            
            // Check if there's a wall blocking the beam
            if (api.hasWallBetween(previousPos, currentPos)) {
                break;
            }
            
            // Check if tile has priority antenna
            if (currentTile != null) {
                for (TileEffect effect : currentTile.getEffects()) {
                    if (effect instanceof Antenna) {
                        break laserPath;
                    }
                }
            }
            
            // Check if there's a robot here
            List<Robot> robotsHere = api.getRobotsOnTile(currentPos.x(), currentPos.y());
            if (!robotsHere.isEmpty()) {
                // Hit the first robot and stop
                Robot target = robotsHere.get(0);
                // Deal damage to the robot
                api.reportDestroy(target.getId(), currentPos,DestroyCause.LASER,power);
                hasTarget = true;
                break;
            }
            
            // Update previous position for next iteration
            previousPos = currentPos;
        }
        if (hasTarget) {
            api.notifyTileEffectActivated(tile.getX(), tile.getY(), "board_laser");
        }
    }

    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_BOARD_LASERS);
    }
}