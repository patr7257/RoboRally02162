package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;

/**
 * @author Weihao Mo
 */
public record AntennaDto(Direction direction) implements EffectDto {
    @Override
    public String kind() {
        return "ANTENNA";
    }

}
