package dk.dtu.domain.rules;

/**
 * @author William Pii Jæger
 */
public record MoveEvent(int robotId, Coord from, Coord to) {
}