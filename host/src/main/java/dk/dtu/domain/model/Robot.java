package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Represents a robot player in the game with position, direction.
 * <p>
 * Each robot maintains also its current position on the board, facing direction, checkpoint progress,
 * and a queue of program operations to execute.
 * </p>
 *
 * @author William Pii Jæger
 * @author Weihao Mo
 * @author Karl Johannes Agerbo
 */
public class Robot {
    private final int id;
    private int x, y;
    private Direction direction;
    private int nextCheckpoint = 1;
    private transient boolean movedOnActivation;
    private Boolean isAlive = true;

    private final Deque<ProgramOP> registers = new ArrayDeque<>();

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public Robot(int id, int x, int y, Direction direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    /**
     * @author Karl Johannes Agerbo
     */
    public Robot(int id, int x, int y, Direction direction, int nextCheckpoint) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.nextCheckpoint = nextCheckpoint;
    }

    /**
     * @author William Pii Jæger
     * @author Weihao Mo
     */
    public void loadProgram(List<ProgramCard> cards) {
        registers.clear();
        for (ProgramCard c : cards) registers.addAll(c.toOps());
    }
    
    public ProgramOP pollNextOp() {
        return registers.pollFirst();
    }

    public boolean hasPendingOps() {
        return !registers.isEmpty();
    }

    public int getNextCheckpoint() {
        return nextCheckpoint;
    }

    /**
     * @author Weihao Mo
     */
    public void advanceCheckpointIfMatches(int checkpointNumber) {
        if (checkpointNumber == nextCheckpoint) {
            nextCheckpoint++;
        }
    }

    /**
     * @author Weihao Mo
     */
    public boolean hasWon(int totalCheckpoints) {
        return nextCheckpoint > totalCheckpoints;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Direction getDirection() {
        return direction;
    }

    public Deque<ProgramOP> getRegisters() {
        return registers;
    }

    public void clearRegisters() {
        registers.clear();
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean movedOnActivation() { return movedOnActivation; }
    public void setMovedOnActivation(boolean v) { movedOnActivation = v; }
    public void setAlive() {
        isAlive = true;
    }

    public void setDead() {
        isAlive = false;
    }

    public Boolean isAlive() {
        return isAlive;
    }
}
