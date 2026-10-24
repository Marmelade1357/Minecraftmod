package de.pf2craft.core.combat;

import de.pf2craft.core.monster.Resistance;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Applies resistances, weaknesses and immunities to rolled damage. */
public final class DamageResolver {
    private static final Set<String> PHYSICAL = Set.of("bludgeoning", "piercing", "slashing");
    private static final Set<String> ENERGY = Set.of(
            "acid", "cold", "electricity", "fire", "sonic", "force", "vitality", "void");
    private static final Set<String> ALL_TYPES = Set.of("bludgeoning", "piercing", "slashing", "acid", "cold",
            "electricity", "fire", "sonic", "force", "vitality", "void", "mental", "poison", "spirit", "bleed");

    private DamageResolver() {
    }

    /** True when the name is a real damage type (lowercase), e.g. "fire" or "slashing". */
    public static boolean isDamageType(String type) {
        return ALL_TYPES.contains(type);
    }

    /** A defence type matches a damage type when equal or via the groups physical, energy and all-damage. */
    static boolean matches(String defenceType, String damageType) {
        return defenceType.equals(damageType)
                || defenceType.equals("all-damage") || defenceType.equals("all")
                || (defenceType.equals("physical") && PHYSICAL.contains(damageType))
                || (defenceType.equals("energy") && ENERGY.contains(damageType));
    }

    /** True when the defence type applies to at least one real damage type (so it is not cold-iron, silver, ...). */
    private static boolean matchesAnyDamageType(String defenceType) {
        for (String t : ALL_TYPES) {
            if (matches(defenceType, t)) {
                return true;
            }
        }
        return false;
    }

    public static DamageOutcome resolve(List<DamageInstance> instances, Defenses defenses) {
        List<AppliedPart> parts = new ArrayList<>();
        int total = 0;
        for (DamageInstance in : instances) {
            boolean immune = false;
            for (String imm : defenses.immunities()) {
                immune |= matches(imm, in.type());
            }
            int weak = 0;
            for (Resistance w : defenses.weaknesses()) {
                if (matches(w.type(), in.type())) {
                    weak = Math.max(weak, w.value());
                }
            }
            int res = 0;
            for (Resistance r : defenses.resistances()) {
                if (matches(r.type(), in.type())) {
                    res = Math.max(res, r.value());
                }
            }
            int applied = 0;
            if (!immune && in.amount() > 0) {
                applied = Math.max(0, in.amount() + weak - res);
            }
            parts.add(new AppliedPart(in.type(), in.amount(), applied));
            total += applied;
        }
        // Defence types that match no damage type at all (cold-iron, silver, critical-hits, ...) have no effect.
        Set<String> unmatched = new LinkedHashSet<>();
        for (Resistance r : defenses.resistances()) {
            if (!matchesAnyDamageType(r.type())) {
                unmatched.add(r.type());
            }
        }
        for (Resistance w : defenses.weaknesses()) {
            if (!matchesAnyDamageType(w.type())) {
                unmatched.add(w.type());
            }
        }
        for (String imm : defenses.immunities()) {
            if (!matchesAnyDamageType(imm)) {
                unmatched.add(imm);
            }
        }
        return new DamageOutcome(total, parts, new ArrayList<>(unmatched));
    }
}
