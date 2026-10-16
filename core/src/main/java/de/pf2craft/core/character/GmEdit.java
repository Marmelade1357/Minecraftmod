package de.pf2craft.core.character;

import de.pf2craft.core.progression.InvalidChoiceException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Validated direct edits a GM may make to a sheet: hp, ability scores and proficiency ranks. */
public final class GmEdit {
    private static final Set<String> RANK_KEYS = buildRankKeys();

    private GmEdit() {
    }

    private static Set<String> buildRankKeys() {
        HashSet<String> keys = new HashSet<>(Set.of("perception", "fortitude", "reflex",
                "will", "class_dc", "unarmored", "light", "medium", "heavy", "simple", "martial",
                "advanced", "unarmed"));
        for (Skill skill : Skill.values()) {
            keys.add(skill.id());
        }
        return Set.copyOf(keys);
    }

    public static CharacterSheet apply(CharacterSheet sheet, String field, int value)
            throws InvalidChoiceException {
        if (field.equals("hp")) {
            return sheet.withCurrentHp(Math.max(0, Math.min(CharacterMath.maxHp(sheet), value)));
        }
        if (field.startsWith("ability:")) {
            Ability ability = parseAbility(field.substring("ability:".length()));
            if (value < 1 || value > 30) {
                throw new InvalidChoiceException("Ability score must be between 1 and 30.");
            }
            return sheet.withAbilityScore(ability, value);
        }
        if (field.startsWith("rank:")) {
            String key = field.substring("rank:".length());
            if (!RANK_KEYS.contains(key)) {
                throw new InvalidChoiceException("Unknown proficiency: " + key);
            }
            Rank[] ranks = Rank.values();
            if (value < 0 || value >= ranks.length) {
                throw new InvalidChoiceException("Rank must be between 0 and " + (ranks.length - 1) + ".");
            }
            return sheet.withRank(key, ranks[value]);
        }
        throw new InvalidChoiceException("Unknown field.");
    }

    private static Ability parseAbility(String key) throws InvalidChoiceException {
        for (Ability ability : Ability.values()) {
            if (ability.pathbuilderKey().equals(key.toLowerCase(Locale.ROOT))) {
                return ability;
            }
        }
        throw new InvalidChoiceException("Unknown ability: " + key);
    }
}
