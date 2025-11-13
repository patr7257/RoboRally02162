package dk.dtu.infrastructure.dto;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public record RobotDto(int id, int x, int y, String facing, int nextCheckpoint) {
}
