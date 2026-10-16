package de.pf2craft.core.character;

import java.util.Locale;

/** The 16 base skills (Lore skills are handled separately). */
public enum Skill {
    ACROBATICS(Ability.DEX),
    ARCANA(Ability.INT),
    ATHLETICS(Ability.STR),
    CRAFTING(Ability.INT),
    DECEPTION(Ability.CHA),
    DIPLOMACY(Ability.CHA),
    INTIMIDATION(Ability.CHA),
    MEDICINE(Ability.WIS),
    NATURE(Ability.WIS),
    OCCULTISM(Ability.INT),
    PERFORMANCE(Ability.CHA),
    RELIGION(Ability.WIS),
    SOCIETY(Ability.INT),
    STEALTH(Ability.DEX),
    SURVIVAL(Ability.WIS),
    THIEVERY(Ability.DEX);

    private final Ability ability;

    Skill(Ability ability) {
        this.ability = ability;
    }

    public Ability ability() {
        return ability;
    }

    /** Lowercase name, equal to the Pathbuilder key. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** English display name, e.g. "Athletics". */
    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
