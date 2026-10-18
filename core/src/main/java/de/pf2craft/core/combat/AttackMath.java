package de.pf2craft.core.combat;

import de.pf2craft.core.check.DegreeOfSuccess;
import java.util.List;
import java.util.OptionalInt;

/** Attack roll arithmetic: multiple attack penalty and degree of success against AC. */
public final class AttackMath {
    private AttackMath() {
    }

    /** Multiple attack penalty as a negative number (or 0) for the n-th attack this turn. */
    public static int mapPenalty(int attackNumber, boolean agile) {
        if (attackNumber <= 1) {
            return 0;
        }
        if (attackNumber == 2) {
            return agile ? -4 : -5;
        }
        return agile ? -8 : -10;
    }

    public static AttackOutcome resolve(int natural, int attackBonus, int mapPenalty, int rangePenalty, int ac) {
        int total = natural + attackBonus + mapPenalty + rangePenalty;
        return new AttackOutcome(natural, total, DegreeOfSuccess.determine(total, ac, OptionalInt.of(natural)));
    }

    public static boolean isAgile(List<String> traits) {
        return traits.contains("agile");
    }
}
