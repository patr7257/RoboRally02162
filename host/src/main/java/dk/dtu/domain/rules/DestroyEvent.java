package dk.dtu.domain.rules;

public record DestroyEvent(int robotId, Coord at, DestroyCause cause) {
}