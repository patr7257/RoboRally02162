package dk.dtu.infrastructure.dto;

/**
 * @author Weihao Mo
 */
public record PitsDto() implements EffectDto{
    @Override
    public String kind() {
        return "PITS";
    }
}
