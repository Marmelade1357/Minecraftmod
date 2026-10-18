package de.pf2craft.core.combat;

import de.pf2craft.core.dice.DiceExpression;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.dice.DieRoller;
import java.util.ArrayList;
import java.util.List;

/** Rolls damage components into damage instances. */
public final class DamageRoller {
    private DamageRoller() {
    }

    /** Each part is dice plus bonus (minimum 0), doubled on a critical hit. */
    public static List<DamageInstance> roll(List<DamageDice> parts, boolean critical, DieRoller roller) {
        List<DamageInstance> out = new ArrayList<>();
        for (DamageDice part : parts) {
            int amount = part.bonus();
            if (!part.dice().isEmpty()) {
                try {
                    amount += DiceExpression.parse(part.dice()).roll(roller).total();
                } catch (DiceParseException e) {
                    throw new IllegalArgumentException("Bad damage dice: " + part.dice(), e);
                }
            }
            amount = Math.max(0, amount);
            out.add(new DamageInstance(part.type(), critical ? amount * 2 : amount));
        }
        return out;
    }
}
