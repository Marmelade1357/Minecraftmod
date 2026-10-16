package de.pf2craft.core.character;

import java.util.OptionalInt;

/** Armor values; {@code armorCategory} is unarmored, light, medium or heavy. */
public record ArmorClass(String armorCategory, OptionalInt dexCap, int itemBonus) {
}
