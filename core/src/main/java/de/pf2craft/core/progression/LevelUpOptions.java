package de.pf2craft.core.progression;

import java.util.List;

/**
 * What a character gets when advancing to {@code newLevel}. {@code featSlots} lists the slot
 * types in the order the chosen feat names must be supplied. {@code hpGain} is a preview
 * computed before ability boosts.
 */
public record LevelUpOptions(int newLevel, boolean skillIncrease, int abilityBoosts,
                             List<String> featSlots, List<String> features, int hpGain) {
    public LevelUpOptions {
        featSlots = List.copyOf(featSlots);
        features = List.copyOf(features);
    }
}
