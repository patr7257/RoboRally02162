package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;

import java.util.List;

public record WallDto(List<Direction> walls) implements EffectDto {
    @Override
    public String kind() { return "WALL"; }
}
