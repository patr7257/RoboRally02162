package dk.dtu.domain.rules;

public record MoveEvent(int robotId, Coord from, Coord to) {
}