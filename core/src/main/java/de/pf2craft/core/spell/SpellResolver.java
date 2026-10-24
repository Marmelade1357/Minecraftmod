package de.pf2craft.core.spell;

import de.pf2craft.core.check.DegreeOfSuccess;

/** Heightened damage and damage multipliers for spells. */
public final class SpellResolver {
    private SpellResolver() {
    }

    /** Base dice plus the heightened dice for the cast rank, merging equal die sizes ("2d4" + 2x"1d4" = "4d4"). */
    public static String damageAtRank(SpellDefinition d, int castRank) {
        String base = d.damageDice();
        if (base.isEmpty() || d.heightenDice().isEmpty() || d.heightenStep() <= 0) {
            return base;
        }
        int steps = Math.max(0, (castRank - Math.max(d.rank(), 1)) / d.heightenStep());
        if (steps == 0) {
            return base;
        }
        int[] b = split(base);
        int[] h = split(d.heightenDice());
        if (b[1] == h[1]) {
            return (b[0] + h[0] * steps) + "d" + b[1];
        }
        return base + "+" + (h[0] * steps) + "d" + h[1];
    }

    private static int[] split(String dice) {
        int i = dice.indexOf('d');
        return new int[] {Integer.parseInt(dice.substring(0, i)), Integer.parseInt(dice.substring(i + 1))};
    }

    /** Damage factor; for SAVE spells the degree is the target's save result. */
    public static double multiplier(SpellKind kind, boolean basicSave, DegreeOfSuccess degree) {
        if (kind == SpellKind.ATTACK) {
            return switch (degree) {
                case CRITICAL_SUCCESS -> 2;
                case SUCCESS -> 1;
                default -> 0;
            };
        }
        if (kind != SpellKind.SAVE) {
            return 0;
        }
        return switch (degree) {
            case CRITICAL_SUCCESS -> 0;
            case SUCCESS -> basicSave ? 0.5 : 0;
            case FAILURE -> 1;
            case CRITICAL_FAILURE -> 2;
        };
    }

    public static int applyFactor(int rolled, double factor) {
        return (int) Math.floor(rolled * factor);
    }
}
