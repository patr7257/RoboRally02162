package dk.dtu.domain.rules;

/**
 * @author William Pii Jæger
 */
public record DestroyEvent(int robotId, Coord at, DestroyCause cause) {
}