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
            String name = baseName(rune);
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

    /** "Greater Flaming (Greater)" and the like: leading grade words and trailing brackets are dropped. */
    private static String baseName(String rune) {
        String name = rune.trim().toLowerCase(Locale.ROOT);
        boolean stripped = true;
        while (stripped) {
            stripped = false;
            for (String grade : new String[] {"greater ", "major ", "true "}) {
                if (name.startsWith(grade)) {
                    name = name.substring(grade.length()).trim();
                    stripped = true;
                }
            }
        }
        return name.split("[ (]", 2)[0];
    }
}
