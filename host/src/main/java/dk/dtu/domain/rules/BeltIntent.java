package dk.dtu.domain.rules;

import dk.dtu.domain.model.Rotation;
import dk.dtu.domain.rules.api.BoardApiImpl;

/**
 * Represents a planned movement for a robot on a conveyor belt.
 * @param robotId the ID of the robot to move
 * @param from the starting coordinate
 * @param to the end coordinate
 * @param speed the number of steps to move (1 for green conveyor, 2 for blue conveyor)
 * @param priority the execution priority (1 for green conveyor, 2 for blue conveyor)
 * @param rotateAfter the rotation to apply after movement (NONE, LEFT, or RIGHT)
 * @see BoardApiImpl#addIntent(BeltIntent)
 * @see BoardApiImpl#resolveIntents()
 * @author Weihao Mo
 */
public record BeltIntent(
        int robotId,
        Coord from,
        Coord to,
        int speed,
        int priority,
        Rotation rotateAfter
) {}