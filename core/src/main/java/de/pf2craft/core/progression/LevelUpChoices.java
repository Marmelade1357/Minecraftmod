package de.pf2craft.core.progression;

import de.pf2craft.core.character.Ability;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The player's decisions for a level-up. {@code skillIncrease} is a skill id or "lore:&lt;n&gt;";
 * {@code featNames} follows the order of {@link LevelUpOptions#featSlots()}.
 */
public record LevelUpChoices(Optional<String> skillIncrease, Set<Ability> boosts,
                             List<String> featNames) {
    public LevelUpChoices {
        boosts = Set.copyOf(boosts);
        featNames = List.copyOf(featNames);
    }
}
