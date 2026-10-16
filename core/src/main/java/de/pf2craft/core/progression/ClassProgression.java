package de.pf2craft.core.progression;

import de.pf2craft.core.character.Rank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Level-by-level data of a class. {@code hpPerLevel} is reference data only; HP math uses the
 * values stored in the character sheet.
 */
public record ClassProgression(String name, int hpPerLevel, Map<Integer, LevelEntry> levels) {
    /** Levels with these slots are identical for every class. */
    public static final List<Integer> ANCESTRY_FEAT_LEVELS = List.of(1, 5, 9, 13, 17);
    public static final List<Integer> GENERAL_FEAT_LEVELS = List.of(3, 7, 11, 15, 19);
    public static final List<Integer> ABILITY_BOOST_LEVELS = List.of(5, 10, 15, 20);

    public ClassProgression {
        levels = Map.copyOf(levels);
    }

    /** One level's gains. Proficiencies only lists fixed rank changes, not choice-based ones. */
    public record LevelEntry(Map<String, Rank> proficiencies, List<String> features,
                             boolean skillIncrease, List<String> featSlots) {
        public LevelEntry {
            proficiencies = Map.copyOf(proficiencies);
            features = List.copyOf(features);
            featSlots = List.copyOf(featSlots);
        }
    }

    /** Fixed ranks reached by the given level; the highest rank per key wins. */
    public Map<String, Rank> ranksAt(int level) {
        Map<String, Rank> result = new HashMap<>();
        for (int l = 1; l <= level; l++) {
            LevelEntry entry = levels.get(l);
            if (entry == null) {
                continue;
            }
            entry.proficiencies().forEach((key, rank) ->
                    result.merge(key, rank, (a, b) -> a.compareTo(b) >= 0 ? a : b));
        }
        return result;
    }
}
