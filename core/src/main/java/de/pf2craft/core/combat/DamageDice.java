package de.pf2craft.core.combat;

/** One damage component: dice like "2d6" (empty for flat damage), a flat bonus and a lowercase damage type. */
public record DamageDice(String dice, int bonus, String type) {
}
