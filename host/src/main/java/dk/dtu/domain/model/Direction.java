package dk.dtu.domain.model;

/**
 * Represents the four directions.
 * This enum provides utility methods for rotating directions, finding opposite directions,
 * and converting between coordinate deltas and directions.
 *
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public enum Direction {
    N,
    E,
    S,
    W;

    /**
     * @author William Pii Jæger
     */
    public Direction turnRight() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    /**
     * @author William Pii Jæger
     */
    public Direction turnLeft() {
        return values()[(this.ordinal() + values().length - 1) % values().length];
    }

    /**
     * @author William Pii Jæger
     */
    public Direction opposite() {
        return values()[(this.ordinal() + 2) % values().length];
    }

    /**
     * @author William Pii Jæger
     */
    public static Direction fromDelta(int x1, int y1, int x2, int y2) {
        if (x1 == x2) {
            if (y2 > y1) {
                return S;
            } else if (y2 < y1) {
                return N;
            }
        } else if (y1 == y2) {
            if (x2 > x1) {
                return E;
            } else if (x2 < x1) {
                return W;
            }
        }
        return null;
    }


}
