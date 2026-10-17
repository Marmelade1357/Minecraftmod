package de.pf2craft.core.character;

/**
 * A weapon attack. {@code category} is a proficiency key (simple, martial, advanced, unarmed).
 * {@code otherAttackBonus} is the item/rune part; {@code otherDamageBonus} is everything
 * except the Strength modifier. {@code damageDice} is the number of weapon dice (striking runes).
 */
public record Strike(String name, String category, String damageDie, String damageType, int damageDice,
                     Ability attackAbility, int otherAttackBonus, int otherDamageBonus) {
}
