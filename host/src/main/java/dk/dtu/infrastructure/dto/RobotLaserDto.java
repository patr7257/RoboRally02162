package dk.dtu.infrastructure.dto;
import dk.dtu.domain.model.Direction;
/**
 * @author Patrick Røbel
 */
public record RobotLaserDto(Direction direction, int robotId) implements EffectDto {
    @Override
    public String kind() {
        return "robot_laser";
    }
}