package de.pf2craft.core.character;

import java.util.Locale;

/** The three saving throws. */
public enum Save {
    FORTITUDE(Ability.CON),
    REFLEX(Ability.DEX),
    WILL(Ability.WIS);

    private final Ability ability;

    Save(Ability ability) {
        this.ability = ability;
    }

    public Ability ability() {
        return ability;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
