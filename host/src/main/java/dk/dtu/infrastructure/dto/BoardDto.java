package dk.dtu.infrastructure.dto;

public record BoardDto(int width, int height, TileDto[][] tiles) {
}
