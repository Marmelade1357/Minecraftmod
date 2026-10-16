package de.pf2craft.core.character;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Test fixtures shared by the character tests. */
final class Fixtures {
    private Fixtures() {
    }

    static CharacterSheet fighter5() {
        Map<Ability, Integer> scores = Map.of(
                Ability.STR, 18, Ability.DEX, 10, Ability.CON, 16,
                Ability.INT, 16, Ability.WIS, 16, Ability.CHA, 10);
        Map<String, Rank> prof = Map.of(
                "perception", Rank.EXPERT, "fortitude", Rank.EXPERT,
                "reflex", Rank.EXPERT, "will", Rank.EXPERT,
                "athletics", Rank.EXPERT, "acrobatics", Rank.TRAINED,
                "heavy", Rank.TRAINED, "martial", Rank.EXPERT,
                "class_dc", Rank.TRAINED);
        return new CharacterSheet("Test Fighter", "Fighter", "Human", "Skilled Human", "Farmhand",
                5, Ability.STR, scores, prof,
                List.of(new Lore("Farming", Rank.TRAINED)),
                new HitPoints(8, 10, 2, 0), 25,
                new ArmorClass("heavy", OptionalInt.of(0), 6),
                List.of(new Strike("Lion Scythe", "martial", "d4", "S", Ability.STR, 0, 0)),
                List.of(new Feat("Power Attack", "Class Feat", 1)),
                List.of("Attack of Opportunity"), 75, false);
    }
}
