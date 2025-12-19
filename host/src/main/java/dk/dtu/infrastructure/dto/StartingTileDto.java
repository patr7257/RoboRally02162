package dk.dtu.infrastructure.dto;

/**
 * @author Patrick Røbel
 */
public record StartingTileDto(int playerId) implements EffectDto {
    @Override
    public String kind() { return "startingtile"; }
}
