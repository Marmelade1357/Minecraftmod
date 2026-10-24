package de.pf2craft.core.spell;

import de.pf2craft.core.check.DegreeOfSuccess;
import java.util.ArrayList;
import java.util.List;

/** Heightened damage and damage multipliers for spells. */
public final class SpellResolver {
    private SpellResolver() {
    }

    /** One damage (or healing) component of a spell at a given rank. */
    public record DamageRoll(String dice, int bonus, String type) {
    }

    /** All damage components at the cast rank: main part first, then the optional extra part. */
    public static List<DamageRoll> damageAt(SpellDefinition d, int castRank) {
        List<DamageRoll> out = new ArrayList<>();
        int steps = d.heightenStep() <= 0 ? 0 : Math.max(0, (castRank - Math.max(d.rank(), 1)) / d.heightenStep());
        if (!d.damageDice().isEmpty() || d.damageBonus() != 0) {
            out.add(new DamageRoll(damageAtRank(d, castRank), d.damageBonus() + steps * d.heightenBonus(),
                    d.damageType()));
        }
        if (!d.extraDice().isEmpty()) {
            out.add(new DamageRoll(merge(d.extraDice(), d.extraHeightenDice(), steps), 0, d.extraType()));
        }
        return out;
    }

    /** Base dice plus the heightened dice for the cast rank, merging equal die sizes ("2d4" + 2x"1d4" = "4d4"). */
    public static String damageAtRank(SpellDefinition d, int castRank) {
        String base = d.damageDice();
        if (d.heightenStep() <= 0) {
            return base;
        }
        return merge(base, d.heightenDice(), Math.max(0, (castRank - Math.max(d.rank(), 1)) / d.heightenStep()));
    }

    private static String merge(String base, String heighten, int steps) {
        if (base.isEmpty() || heighten.isEmpty() || steps == 0) {
            return base;
        }
        int[] b = split(base);
        int[] h = split(heighten);
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
        if (kind == SpellKind.AUTO) {
            return 1;
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
