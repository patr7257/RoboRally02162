package dk.dtu.domain.rules;

import dk.dtu.domain.model.Rotation;

public record BeltIntent(
        int robotId,
        Coord from,
        Coord to,
        int speed,
        int priority,
        Rotation rotateAfter
) {}