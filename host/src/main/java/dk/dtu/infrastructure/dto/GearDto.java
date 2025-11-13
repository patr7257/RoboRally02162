package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Rotation;

/**
 * @author Weihao Mo
 */
public record GearDto(Rotation rotation) implements EffectDto {
    @Override
    public String kind() {
        return "GEAR";
    }
}
