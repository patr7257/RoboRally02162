package dk.dtu.infrastructure.dto;

import dk.dtu.domain.model.Direction;

import java.util.List;

/**
 * @author Patrick Røbel
 */
public record StartingTileDto(int playerId) implements EffectDto {
    @Override
    public String kind() { return "startingtile"; }
}
