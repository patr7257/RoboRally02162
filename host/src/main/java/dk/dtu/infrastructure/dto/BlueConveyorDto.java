package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Rotation;

/**
 * @author Weihao Mo
 */
public record BlueConveyorDto(Direction direction, Rotation rotation) implements EffectDto{
    @Override public String kind() { return "BLUE_CONVEYOR"; }
}
