package de.pf2craft.core.combat;

/** Coarse health state shown to players instead of exact hit points. */
public enum HealthLabel {
    UNHURT,
    HURT,
    BADLY_HURT,
    DEFEATED;

    public static HealthLabel of(int current, int max) {
        if (current <= 0) {
            return DEFEATED;
        }
        if (current >= max) {
            return UNHURT;
        }
        return current * 2 < max ? BADLY_HURT : HURT;
    }
}
