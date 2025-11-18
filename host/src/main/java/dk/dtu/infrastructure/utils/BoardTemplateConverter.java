package dk.dtu.infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dtu.domain.model.*;
import dk.dtu.domain.rules.effects.*;

import java.util.*;

/**
 * Helper Class - Converts board templates from JSON to domain objects.
 * 
 * @author Patrick Røbel
 */
public class BoardTemplateConverter {
    
    // Converts a JSON board template to a Board domain object.
    public static Board convertTemplateToBoard(JsonNode template) {
        // boardWidth and boardHeight represent the GAME AREA dimensions
        int gameAreaWidth = template.get("boardWidth").asInt();
        int gameAreaHeight = template.get("boardHeight").asInt();
        
        // Get starting area dimensions
        int startingWidth = template.has("startingBoardWidth") ? template.get("startingBoardWidth").asInt() : 0;
        int startingHeight = template.has("startingBoardHeight") ? template.get("startingBoardHeight").asInt() : 0;
        String direction = template.has("startingBoardDirection") ? template.get("startingBoardDirection").asText().toUpperCase() : "W";
        
        // Calculate actual board dimensions by adding starting area to game area
        int actualWidth, actualHeight;
        if (direction.equals("W") || direction.equals("E")) {
            // Starting area is on left or right - add to width
            actualWidth = gameAreaWidth + startingWidth;
            actualHeight = gameAreaHeight;
        } else {
            // Starting area is on top or bottom - add to height
            actualWidth = gameAreaWidth;
            actualHeight = gameAreaHeight + startingHeight;
        }
        
        // Create empty board with actual dimensions
        Tile[][] tiles = new Tile[actualWidth][actualHeight];
        for (int x = 0; x < actualWidth; x++) {
            for (int y = 0; y < actualHeight; y++) {
                tiles[x][y] = new Tile(x, y);
            }
        }
        
        Board board = new Board(actualWidth, actualHeight, tiles);
        
        // Apply effects from template
        if (template.has("effects")) {
            JsonNode effects = template.get("effects");
            Iterator<String> fieldNames = effects.fieldNames();
            
            while (fieldNames.hasNext()) {
                String coords = fieldNames.next();
                
                // Skip comment keys (start with underscore)
                if (coords.startsWith("_")) {
                    continue;
                }
                
                String[] parts = coords.split(",");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                
                if (board.isInBounds(x, y)) {
                    JsonNode effectList = effects.get(coords);
                    for (JsonNode effectNode : effectList) {
                        TileEffect effect = convertEffect(effectNode);
                        if (effect != null) {
                            board.getTile(x, y).addEffect(effect);
                        }
                    }
                }
            }
        }
        
        return board;
    }
    
    // Converts a JSON effect to a TileEffect domain object.
    private static TileEffect convertEffect(JsonNode effectNode) {
        String kind = effectNode.get("kind").asText().toLowerCase();
        
        return switch (kind) {
            case "antenna" -> {
                Direction dir = Direction.valueOf(effectNode.get("direction").asText());
                yield new Antenna(dir);
            }
            case "startingtile" -> {
                int playerId = effectNode.get("playerId").asInt();
                yield new StartingTile(playerId);
            }
            case "checkpoint" -> {
                int number = effectNode.get("number").asInt();
                yield new Checkpoint(number);
            }
            case "reboot_token" -> {
                Direction dir = Direction.valueOf(effectNode.get("direction").asText());
                yield new RebootToken(dir);
            }
            case "walldto" -> {
                EnumSet<Direction> walls = EnumSet.noneOf(Direction.class);
                JsonNode wallsArray = effectNode.get("walls");
                for (JsonNode wall : wallsArray) {
                    walls.add(Direction.valueOf(wall.asText()));
                }
                yield new Walls(walls);
            }
            case "green_conveyor" -> {
                Direction dir = Direction.valueOf(effectNode.get("direction").asText());
                Rotation rot = Rotation.valueOf(effectNode.get("rotation").asText());
                yield new GreenConveyor(dir, rot);
            }
            case "blue_conveyor" -> {
                Direction dir = Direction.valueOf(effectNode.get("direction").asText());
                Rotation rot = Rotation.valueOf(effectNode.get("rotation").asText());
                yield new BlueConveyor(dir, rot);
            }
            case "gear" -> {
                Rotation rot = Rotation.valueOf(effectNode.get("rotation").asText());
                yield new Gear(rot);
            }
            default -> {
                System.err.println("Unknown effect kind: " + kind);
                yield null;
            }
        };
    }
    
    // Extracts starting positions from a board template and creates robots.
    // Randomizes both the starting tile positions and robot IDs to ensure varied gameplay.
    public static List<Robot> createRobotsFromTemplate(Board board, int playerCount) {
        List<Robot> robots = new ArrayList<>();
        
        // Find all starting tiles with their positions
        List<TilePosition> startingPositions = new ArrayList<>();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Tile tile = board.getTile(x, y);
                for (TileEffect effect : tile.getEffects()) {
                    if (effect instanceof StartingTile st) {
                        startingPositions.add(new TilePosition(x, y));
                        break; // Only need one starting tile per position
                    }
                }
            }
        }
        
        // Shuffle starting positions for randomization
        Collections.shuffle(startingPositions);
        
        // Create list of robot IDs and shuffle them too
        List<Integer> robotIds = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            robotIds.add(i);
        }
        Collections.shuffle(robotIds);
        
        // Create robots at shuffled positions with shuffled IDs
        for (int i = 0; i < Math.min(playerCount, startingPositions.size()); i++) {
            TilePosition pos = startingPositions.get(i);
            int robotId = robotIds.get(i);
            robots.add(new Robot(robotId, pos.x, pos.y, Direction.S));
        }
        
        return robots;
    }
    
    // Helper class to store tile positions for random robot placement each time.
    private static class TilePosition {
        final int x;
        final int y;
        
        TilePosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
