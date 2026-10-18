package de.pf2craft.core.combat;

import java.util.List;
import java.util.OptionalInt;

/** Melee reach and ranged-increment rules. One block is 5 feet. */
public final class RangeRules {
    private static final int MAX_INCREMENTS = 6;

    private RangeRules() {
    }

    /** Melee reach in blocks, measured hitbox edge to hitbox edge by the caller. */
    public static double meleeReachBlocks(List<String> traits) {
        if (traits.contains("reach-20")) {
            return 4.0;
        }
        if (traits.contains("reach-15")) {
            return 3.0;
        }
        if (traits.contains("reach") || traits.contains("reach-10")) {
            return 2.0;
        }
        return 1.0;
    }

    /** Range penalty (negative or 0), or empty when the target is beyond six range increments. */
    public static OptionalInt rangePenalty(double distanceBlocks, int incrementFeet) {
        int increments = Math.max(1, (int) Math.ceil(distanceBlocks * 5 / incrementFeet));
        if (increments > MAX_INCREMENTS) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(-2 * (increments - 1));
    }
}
