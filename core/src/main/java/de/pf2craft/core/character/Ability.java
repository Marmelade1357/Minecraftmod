package de.pf2craft.core.character;

import java.util.Locale;

/** The six Pathfinder 2e ability scores. */
public enum Ability {
    STR, DEX, CON, INT, WIS, CHA;

    /** Ability modifier for a score: (score - 10) / 2, rounded down. */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    /** Lowercase key as used in Pathbuilder exports, e.g. "str". */
    public String pathbuilderKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Ability fromPathbuilder(String key) {
        return valueOf(key.trim().toUpperCase(Locale.ROOT));
    }
}
