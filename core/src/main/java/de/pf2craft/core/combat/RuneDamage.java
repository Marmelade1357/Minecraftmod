package de.pf2craft.core.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Extra damage from elemental property runes. */
public final class RuneDamage {
    private RuneDamage() {
    }

    public static List<DamageDice> parts(List<String> propertyRunes) {
        List<DamageDice> out = new ArrayList<>();
        for (String rune : propertyRunes) {
            String name = rune.trim().toLowerCase(Locale.ROOT).split("[\s(]", 2)[0];
            String type = switch (name) {
                case "flaming" -> "fire";
                case "frost" -> "cold";
                case "shock" -> "electricity";
                case "corrosive" -> "acid";
                case "thundering" -> "sonic";
                default -> null;
            };
            if (type != null) {
                out.add(new DamageDice("1d6", 0, type));
            }
        }
        return out;
    }
}
