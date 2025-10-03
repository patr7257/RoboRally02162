package dk.dtu.infrastructure.dto;

public record CheckpointDto(int number) implements EffectDto {
    @Override
    public String kind() {
        return "CHECKPOINT";
    }
}
