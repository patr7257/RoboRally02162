package dk.dtu.infrastructure.dto;

/**
 * @author William Pii Jæger
 * @author Weihao Mo
 */
public record BoardDto(int width, int height, TileDto[][] tiles) {
}
