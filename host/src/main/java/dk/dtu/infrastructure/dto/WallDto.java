package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;

import java.util.List;

/**
 * @author William Pii Jæger
 */
public record WallDto(List<Direction> walls) implements EffectDto {
    @Override
    public String kind() { return "WALL"; }
}
