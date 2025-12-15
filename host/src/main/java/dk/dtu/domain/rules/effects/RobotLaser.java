package dk.dtu.domain.rules.effects;

import java.util.*;
import dk.dtu.domain.core.Phase;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.api.BoardAPI;

/**
 * Robot Laser effect that shoots from a robot's current position at the end of each turn.
 * Each robot shoots a laser in the direction it's facing, hitting the first robot in line of sight.
 * Robot lasers always deal 1 damage (SPAM card).
 *
 * @param robot the robot that shoots the laser
 * 
 * @author Patrick Røbel
 */

public record RobotLaser(Robot robot) implements TileEffect {

    /**
     * When the robot laser phase activates at the end of the turn, each robot shoots
     * a laser from its current position in the direction it's facing.
     * 
     * @author Patrick Røbel
     */
    @Override
    public void onPhase(Phase phase, Tile tile, BoardAPI api) {
        if (phase != Phase.ACTIVATE_ROBOT_LASERS) return;
        
        boolean hasTarget = false;
        
        // Pre calculate laser sight starting from LaserPosition
        Coord currentPos = new Coord(tile.getX(), tile.getY());
        Coord previousPos = currentPos;
        
        Direction direction = robot.getDirection();
        
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
                // Hit the first robot (but not ourselves) and stop
                Robot target = robotsHere.get(0);
                // Don't hit the robot that fired the laser
                if (target.getId() != robot.getId()) {
                    // Deal 1 damage (SPAM card) to the robot
                    api.reportDestroy(target.getId(), currentPos, DestroyCause.LASER, 1);
                    hasTarget = true;
                }
                break;
            }

            // Update previous position for next iteration
            previousPos = currentPos;
        }
        if (hasTarget) {
            api.notifyTileEffectActivated(tile.getX(), tile.getY(), "robot_laser");
        }
    }

    /**
     * @author Patrick Røbel
     */
    public static void applyRobotLaserEffects(Phase phase, List<Robot> robots, dk.dtu.domain.model.Board board, BoardAPI api) {
        // Add robot laser effects for all alive robots
        Map<Tile, List<TileEffect>> addedEffects = new HashMap<>();
        for (Robot robot : robots) {
            if (robot.isAlive()) {
                Tile tile = board.getTile(robot.getX(), robot.getY());
                if (tile != null) {
                    RobotLaser laserEffect = new RobotLaser(robot);
                    tile.addEffect(laserEffect);
                    addedEffects.computeIfAbsent(tile, k -> new ArrayList<>()).add(laserEffect);
                }
            }
        }
        
        // Execute robot laser effects
        for (Tile tile : addedEffects.keySet()) {
            for (TileEffect effect : tile.getEffectsForPhase(phase)) {
                if (effect instanceof RobotLaser) {
                    effect.onPhase(phase, tile, api);
                }
            }
        }
        
        // Remove robot laser effects
        for (Map.Entry<Tile, List<TileEffect>> entry : addedEffects.entrySet()) {
            for (TileEffect effect : entry.getValue()) {
                entry.getKey().removeEffect(effect);
            }
        }
    }
    
    @Override
    public EnumSet<Phase> phases() {
        return EnumSet.of(Phase.ACTIVATE_ROBOT_LASERS);
    }
}