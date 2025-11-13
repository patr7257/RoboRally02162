package dk.dtu.infrastructure.dto;

/**
 * @author Weihao Mo
 */
public record CheckpointDto(int number) implements EffectDto {
    @Override
    public String kind() {
        return "CHECKPOINT";
    }
}
