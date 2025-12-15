package dk.dtu.domain.core;


/**
 * Enumeration of game phases.
 * Phase execution order follows:
 * ACTIVATION → Board effect phases (conveyors, gears, lasers, checkpoints, pits, reboot).
 * Robot lasers (#ACTIVATE_ROBOT_LASERS) are applied during the sequence despite being
 * dynamic effects rather than tile effects, to maintain correct game rule ordering.
 *
 * @see Game#runPhase(Phase, Runnable)
 * @see Game#applyTileEffects(Phase)
 * @author Weihao Mo
 * @author William Pii Jæger
 */
public enum Phase {
    UPGRADE,
    PROGRAMMING,
    ACTIVATION,
    ACTIVATE_BLUECONVEYOR,
    ACTIVATE_GREENCONVEYOR,
    ACTIVATE_GEAR,
    ACTIVATE_BOARD_LASERS,
    ACTIVATE_ROBOT_LASERS,
    ACTIVATE_CHECKPOINTS,
    ACTIVATE_PITS,
    ACTIVATE_REBOOT,
    ACTIVATE_ANTENNA
}
