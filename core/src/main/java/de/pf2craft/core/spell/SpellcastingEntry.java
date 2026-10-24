package de.pf2craft.core.spell;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.Rank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One spellcasting entry (e.g. "Wizard" or "Focus Spells"). {@code perDay} has length 11 (index =
 * spell rank 0..10); {@code spellsByRank} maps spell rank to spell names.
 */
public record SpellcastingEntry(String name, String tradition, CastingType type, Ability ability,
                                Rank rank, int[] perDay, Map<Integer, List<String>> spellsByRank) {
    public static final int RANKS = 11;

    public SpellcastingEntry {
        int[] days = new int[RANKS];
        if (perDay != null) {
            System.arraycopy(perDay, 0, days, 0, Math.min(perDay.length, RANKS));
        }
        perDay = days;
        Map<Integer, List<String>> copy = new TreeMap<>();
        if (spellsByRank != null) {
            spellsByRank.forEach((k, v) -> copy.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        }
        spellsByRank = Collections.unmodifiableMap(copy);
    }

    @Override
    public int[] perDay() {
        return perDay.clone();
    }
}
