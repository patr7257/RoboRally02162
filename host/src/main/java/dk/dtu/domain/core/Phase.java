package dk.dtu.domain.core;


/**
 * Enumeration of game phases.
 * Phase execution order follows:
 * ACTIVATION → Board effect phases (conveyors, checkpoints, etc.)
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
    ACTIVATE_CHECKPOINTS,
    ACTIVATE_REBOOT,
    ACTIVATE_ANTENNA
}
