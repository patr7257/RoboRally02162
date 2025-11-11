package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;
import dk.dtu.domain.model.Rotation;

public record GreenConveyorDto(Direction direction, Rotation rotation) implements EffectDto{
    @Override public String kind() { return "GREEN_CONVEYOR"; }
}
