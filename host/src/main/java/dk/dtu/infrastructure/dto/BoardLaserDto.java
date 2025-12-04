package dk.dtu.infrastructure.dto;
import dk.dtu.domain.model.Direction;
/**
 * @author Patrick Røbel
 */
public record BoardLaserDto(Direction direction, int power) implements EffectDto {
    @Override
    public String kind() {
        return "board_laser";
    }
}
