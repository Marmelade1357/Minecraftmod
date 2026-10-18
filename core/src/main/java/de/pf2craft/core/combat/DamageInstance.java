package de.pf2craft.core.combat;

/** One rolled amount of damage of a single (lowercase) damage type. */
public record DamageInstance(String type, int amount) {
}
