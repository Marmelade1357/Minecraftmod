package de.pf2craft.core.spell;

import java.util.Locale;

/** How a spellcasting entry gets its spells. */
public enum CastingType {
    PREPARED, SPONTANEOUS, FOCUS, INNATE;

    /** Maps Pathbuilder's spellcastingType and magicTradition strings; unknown values mean prepared. */
    public static CastingType fromPathbuilder(String type, String tradition) {
        String tr = tradition == null ? "" : tradition.trim().toLowerCase(Locale.ROOT);
        String ty = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (tr.equals("focus")) {
            return FOCUS;
        }
        if (tr.equals("innate") || ty.equals("innate")) {
            return INNATE;
        }
        if (ty.equals("spontaneous")) {
            return SPONTANEOUS;
        }
        return PREPARED;
    }
}
