package de.pf2craft.core.combat;

/** Numeric effects of conditions. Penalties are returned as values of 0 or less. */
public final class ConditionEffects {
    private ConditionEffects() {}

    /** Status penalty to all checks and DCs (frightened). */
    public static int checkPenalty(ConditionSet s) {
        return -s.value(Condition.FRIGHTENED);
    }

    /** Armor class: status penalty plus a circumstance -2 that applies once. */
    public static int acPenalty(ConditionSet s) {
        boolean circumstance = s.has(Condition.OFF_GUARD) || s.has(Condition.PRONE)
                || s.has(Condition.GRABBED) || s.has(Condition.UNCONSCIOUS);
        return checkPenalty(s) + (circumstance ? -2 : 0);
    }

    /** Attack rolls: status penalty plus -2 while prone. */
    public static int attackPenalty(ConditionSet s) {
        return checkPenalty(s) + (s.has(Condition.PRONE) ? -2 : 0);
    }

    /** Actions lost at the start of the turn (slowed + stunned), at most 3. */
    public static int actionsLost(ConditionSet s) {
        return Math.min(3, s.value(Condition.SLOWED) + s.value(Condition.STUNNED));
    }

    public static boolean canAct(ConditionSet s) {
        return !s.has(Condition.UNCONSCIOUS) && !s.has(Condition.DYING);
    }
}
