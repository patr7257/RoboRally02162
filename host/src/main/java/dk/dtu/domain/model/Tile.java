package dk.dtu.domain.model;

import dk.dtu.domain.core.Phase;
import dk.dtu.domain.rules.effects.TileEffect;

import java.util.ArrayList;
import java.util.List;

// Author(s) Weihao Mo, William Pii Jæger

public class Tile {
    private int x, y;

    private List<TileEffect> effects;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.effects = new ArrayList<>();
    }
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setEffects(List<TileEffect> effects) {
        this.effects = effects;
    }

    public void addEffect(TileEffect effect) {
        this.effects.add(effect);
    }

    public void removeEffect(TileEffect effect) {
        this.effects.remove(effect);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<TileEffect> getEffects() {
        return effects;
    }

    public List<TileEffect> getEffectsForPhase(Phase phase) {
        List<TileEffect> out = new ArrayList<>();
        for (TileEffect e : effects) {
            var phases = e.phases();
            if (phases != null && phases.contains(phase)) {
                out.add(e);
            }
        }
        return List.copyOf(out);
    }
}
