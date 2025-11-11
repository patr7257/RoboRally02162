package dk.dtu.domain.model;

// Author(s) Weihao Mo, William Pii Jæger

public enum Direction {
    N,
    E,
    S,
    W;

    public Direction turnRight() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public Direction turnLeft() {
        return values()[(this.ordinal() + values().length - 1) % values().length];
    }

    public Direction opposite() {
        return values()[(this.ordinal() + 2) % values().length];
    }

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

    public int dx(Direction d) {
        return switch (d) {
            case E -> 1;
            case W -> -1;
            case N -> 0;
            case S -> 0;
        };
    }
    public int dy(Direction d) {
        return switch (d) {
            case E -> 0;
            case W -> 0;
            case N -> -1;
            case S -> 1;
        };
    }
}
