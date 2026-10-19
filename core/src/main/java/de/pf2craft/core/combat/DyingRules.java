package de.pf2craft.core.combat;

import de.pf2craft.core.check.DegreeOfSuccess;

/** Knocked-out, dying and recovery rules. All methods mutate the passed condition set. */
public final class DyingRules {
    private DyingRules() {}

    public record RecoveryResult(boolean stabilized, boolean wouldBeDead) {}

    /** Unconscious and dying 1 (2 on a critical hit), plus the wounded value. */
    public static void knockOut(ConditionSet s, boolean critical) {
        s.set(Condition.UNCONSCIOUS, 1);
        s.set(Condition.DYING, (critical ? 2 : 1) + s.value(Condition.WOUNDED));
    }

    public static int recoveryDc(ConditionSet s) {
        return 10 + s.value(Condition.DYING);
    }

    public static RecoveryResult recover(ConditionSet s, DegreeOfSuccess d) {
        int change = switch (d) {
            case CRITICAL_SUCCESS -> -2;
            case SUCCESS -> -1;
            case FAILURE -> 1;
            case CRITICAL_FAILURE -> 2;
        };
        int dying = s.value(Condition.DYING) + change;
        if (dying <= 0) {
            s.remove(Condition.DYING);
            s.set(Condition.WOUNDED, s.value(Condition.WOUNDED) + 1);
            return new RecoveryResult(true, false);
        }
        s.set(Condition.DYING, dying);
        return new RecoveryResult(false, dying >= ConditionSet.DYING_CAP);
    }

    /** Healing ends dying and unconsciousness; surviving dying raises wounded. */
    public static void heal(ConditionSet s) {
        boolean wasDying = s.has(Condition.DYING);
        s.remove(Condition.DYING);
        s.remove(Condition.UNCONSCIOUS);
        if (wasDying) {
            s.set(Condition.WOUNDED, s.value(Condition.WOUNDED) + 1);
        }
    }
}
