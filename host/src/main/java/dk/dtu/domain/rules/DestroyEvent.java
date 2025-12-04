package dk.dtu.domain.rules;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public record DestroyEvent(int robotId, Coord at, DestroyCause cause, int power) {
    public DestroyEvent(int robotId, Coord at, DestroyCause cause) {
        this(robotId,at,cause,0);
    }
 }