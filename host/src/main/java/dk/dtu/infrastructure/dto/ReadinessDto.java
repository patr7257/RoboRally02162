package dk.dtu.infrastructure.dto;

import java.util.Map;

/**
 * @author William Pii Jæger
 */
public record ReadinessDto(
        Map<Integer, Boolean> playerSubmitted,
        long msRemaining
) {
}