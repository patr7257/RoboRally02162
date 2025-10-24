package dk.dtu.infrastructure.dto;

import java.util.Map;

// Author(s) William Pii Jæger

public record ReadinessDto(
        Map<Integer, Boolean> playerSubmitted,
        long msRemaining
) {
}