package dk.dtu.infrastructure.utils;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Rotation;
import dk.dtu.domain.rules.effects.BlueConveyor;
import dk.dtu.domain.rules.effects.GreenConveyor;

/**
 * @author Weihao Mo
 */
public class ConveyorBeltPatterns {

    public static void applyHighOctane(Board b) {
        for (int x = 2; x <= 6; x++) {
            b.getTile(x, 3).addEffect(new BlueConveyor(Direction.E, Rotation.NONE));
        }
        b.getTile(7, 3).addEffect(new BlueConveyor(Direction.E, Rotation.NONE));
        b.getTile(8, 3).addEffect(new BlueConveyor(Direction.S, Rotation.RIGHT));

        for (int y = 4; y <= 9; y++) {
            b.getTile(8, y).addEffect(new BlueConveyor(Direction.S, Rotation.NONE));
        }
        b.getTile(8, 10).addEffect(new BlueConveyor(Direction.W, Rotation.RIGHT));

        for (int x = 7; x >= 2; x--) {
            b.getTile(x, 10).addEffect(new BlueConveyor(Direction.W, Rotation.NONE));
        }
        b.getTile(1, 10).addEffect(new BlueConveyor(Direction.N, Rotation.RIGHT));

        for (int y = 9; y >= 4; y--) {
            b.getTile(1, y).addEffect(new BlueConveyor(Direction.N, Rotation.NONE));
        }
        b.getTile(1, 3).addEffect(new BlueConveyor(Direction.E, Rotation.RIGHT));

        for (int x = 3; x <= 5; x++) {
            b.getTile(x, 4).addEffect(new GreenConveyor(Direction.E, Rotation.NONE));
        }
        b.getTile(6, 4).addEffect(new GreenConveyor(Direction.E, Rotation.NONE));
        b.getTile(7, 4).addEffect(new GreenConveyor(Direction.S, Rotation.RIGHT));

        for (int y = 5; y <= 8; y++) {
            b.getTile(7, y).addEffect(new GreenConveyor(Direction.S, Rotation.NONE));
        }
        b.getTile(7, 9).addEffect(new GreenConveyor(Direction.W, Rotation.RIGHT));

        for (int x = 6; x >= 3; x--) {
            b.getTile(x, 9).addEffect(new GreenConveyor(Direction.W, Rotation.NONE));
        }
        b.getTile(2, 9).addEffect(new GreenConveyor(Direction.N, Rotation.RIGHT));

        for (int y = 8; y >= 5; y--) {
            b.getTile(2, y).addEffect(new GreenConveyor(Direction.N, Rotation.NONE));
        }
        b.getTile(2, 4).addEffect(new GreenConveyor(Direction.E, Rotation.RIGHT));
    }

    public static void applyCorridorBlitz(Board b) {
        int left = 3, top = 3, right = 6, bottom = 6;

        for (int x = left + 1; x <= right - 1; x++) {
            b.getTile(x, top + 2).addEffect(new BlueConveyor(Direction.E, Rotation.NONE));
        }
        b.getTile(right, top + 2).addEffect(new BlueConveyor(Direction.S, Rotation.RIGHT));

        for (int y = top + 3; y <= bottom + 1; y++) {
            b.getTile(right, y).addEffect(new BlueConveyor(Direction.S, Rotation.NONE));
        }
        b.getTile(right, bottom + 2).addEffect(new BlueConveyor(Direction.W, Rotation.RIGHT));

        for (int x = right - 1; x >= left + 1; x--) {
            b.getTile(x, bottom + 2).addEffect(new BlueConveyor(Direction.W, Rotation.NONE));
        }
        b.getTile(left, bottom + 2).addEffect(new BlueConveyor(Direction.N, Rotation.RIGHT));

        for (int y = bottom + 1; y >= top + 3; y--) {
            b.getTile(left, y).addEffect(new BlueConveyor(Direction.N, Rotation.NONE));
        }
        b.getTile(left, top + 2).addEffect(new BlueConveyor(Direction.E, Rotation.RIGHT));

        b.getTile(1, 4).addEffect(new GreenConveyor(Direction.E, Rotation.NONE));
        b.getTile(2, 4).addEffect(new GreenConveyor(Direction.E, Rotation.NONE));

        b.getTile(7, 6).addEffect(new GreenConveyor(Direction.S, Rotation.NONE));
        b.getTile(7, 7).addEffect(new GreenConveyor(Direction.S, Rotation.NONE));

        b.getTile(1, 9).addEffect(new GreenConveyor(Direction.E, Rotation.NONE));
        b.getTile(2, 9).addEffect(new GreenConveyor(Direction.E, Rotation.NONE));

        b.getTile(2, 7).addEffect(new GreenConveyor(Direction.N, Rotation.NONE));
        b.getTile(2, 6).addEffect(new GreenConveyor(Direction.N, Rotation.NONE));
    }

}