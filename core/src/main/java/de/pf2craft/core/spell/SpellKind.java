package de.pf2craft.core.spell;

/** How a spell is resolved: spell attack roll, saving throw, healing, or no damage at all. */
public enum SpellKind {
    ATTACK,
    SAVE,
    HEAL,
    UTILITY
}
