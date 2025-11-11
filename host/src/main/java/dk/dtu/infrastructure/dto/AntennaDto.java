package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;

public record AntennaDto(Direction direction) implements EffectDto {
    @Override
    public String kind() {
        return "ANTENNA";
    }

}
