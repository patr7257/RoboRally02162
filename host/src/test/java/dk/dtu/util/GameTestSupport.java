package dk.dtu.util;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.*;
import dk.dtu.domain.rules.effects.Walls;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameTestSupport {

    public static List<Robot> lineRobots(int startId, int startX, int y, int count, Direction facing) {
        List<Robot> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(new Robot(startId + i, startX + i, y, facing));
        }
        return out;
    }

    public static void walls(Board b, int x, int y, Direction... dirs) {
        b.getTile(x, y).setEffects(List.of(new Walls(EnumSet.copyOf(List.of(dirs)))));
    }

    public static void assertPosDir(Robot r, int x, int y, Direction d) {
        assertEquals(x, r.getX(), "x");
        assertEquals(y, r.getY(), "y");
        assertEquals(d, r.getDirection(), "dir");
    }

    public static Outcome.Moved assertMoved(Outcome out) {
        assertInstanceOf(Outcome.Moved.class, out);
        return (Outcome.Moved) out;
    }

    public static Outcome.Blocked assertBlocked(Outcome out) {
        assertInstanceOf(Outcome.Blocked.class, out);
        return (Outcome.Blocked) out;
    }

    public static void assertEdgeBlock(Outcome out, Edge expected) {
        Outcome.Blocked b = assertBlocked(out);
        assertInstanceOf(EdgeBlock.class, b.reason());
        EdgeBlock eb = (EdgeBlock) b.reason();
        assertEquals(expected, eb.edge());
    }

    public static void assertChainBlockedByEdge(Outcome out, List<Integer> chain, Edge expectedStop) {
        Outcome.Blocked b = assertBlocked(out);
        assertInstanceOf(RobotChainImmovable.class, b.reason());
        RobotChainImmovable rci = (RobotChainImmovable) b.reason();
        assertEquals(chain, rci.chain());
        assertInstanceOf(EdgeBlock.class, rci.stop());
        EdgeBlock stop = (EdgeBlock) rci.stop();
        assertEquals(expectedStop, stop.edge());
    }

    public static void assertMove(Outcome.Moved moved, int idx, int robotId, Coord from, Coord to) {
        MoveEvent ev = moved.moves().get(idx);
        assertEquals(robotId, ev.robotId());
        assertEquals(from, ev.from());
        assertEquals(to, ev.to());
    }

    public static void assertDestroy(Outcome.Moved moved, int idx, int robotId, Coord at) {
        DestroyEvent ev = moved.destroys().get(idx);
        assertEquals(robotId, ev.robotId());
        assertEquals(at, ev.at());
    }
}