package de.pf2craft.core.monster;

/** One damage component of a strike: dice like "2d8" (empty for flat damage), a bonus and a damage type. */
public record DamagePart(String dice, int bonus, String type) {
}
