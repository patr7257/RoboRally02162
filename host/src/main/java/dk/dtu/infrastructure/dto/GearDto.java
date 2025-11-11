package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Rotation;

public record GearDto(Rotation rotation) implements EffectDto {
    @Override
    public String kind() {
        return "GEAR";
    }
}
