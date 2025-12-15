package dk.dtu.domain.core;

/**
 * Configuration for custom timing delays in demo mode.
 * Allows fine-grained control over execution speed for demonstrations.
 *
 * @param registerDelayMs delay between registers
 * @param preRoundDelayMs delay before starting a round
 * @param effectDelayMs delay after applying board effects
 * @param robotTurnDelayMs delay between robot turns
 * @param respawnTimeoutMs timeout for respawn direction selection
 * @param reactionTimeoutMs timeout for reaction choices
 *
 * @author William Pii Jæger
 */
public record DemoTimingConfig(
        long registerDelayMs,
        long preRoundDelayMs,
        long effectDelayMs,
        long robotTurnDelayMs,
        long respawnTimeoutMs,
        long reactionTimeoutMs
) {
    public static DemoTimingConfig defaultConfig() {
        return new DemoTimingConfig(800, 300, 500, 400, 10000, 20000);
    }
}