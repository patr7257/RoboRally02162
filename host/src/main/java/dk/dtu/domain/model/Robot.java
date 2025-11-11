package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

// Author(s) William Pii Jæger, Weihao Mo

public class Robot {
    private final int id;
    private int x, y;
    private Direction direction;
    private int nextCheckpoint = 1;
    private transient boolean movedOnActivation;
    private Boolean isAlive = true;

    private final Deque<ProgramOP> registers = new ArrayDeque<>();

    public Robot(int id, int x, int y, Direction direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

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

    public void advanceCheckpointIfMatches(int checkpointNumber) {
        if (checkpointNumber == nextCheckpoint) {
            nextCheckpoint++;
        }
    }

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
