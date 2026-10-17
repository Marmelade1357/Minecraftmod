package de.pf2craft.core.character;

/** A worn shield with its statistics. */
public record Shield(String name, int acBonus, int hardness, int hitPoints, int brokenThreshold) {
}
