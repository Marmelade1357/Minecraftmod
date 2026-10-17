package de.pf2craft.core.character;

import java.util.List;

/**
 * A weapon attack. {@code category} is a proficiency key (simple, martial, advanced, unarmed).
 * {@code otherAttackBonus} is the item/rune part; {@code otherDamageBonus} is everything
 * except the Strength modifier. {@code damageDice} is the number of weapon dice (striking runes).
 * {@code group} and {@code traits} come from the item catalog (empty when unknown); potency and
 * property runes come from the Pathbuilder export.
 */
public record Strike(String name, String category, String damageDie, String damageType, int damageDice,
                     Ability attackAbility, int otherAttackBonus, int otherDamageBonus,
                     String group, List<String> traits, List<String> propertyRunes, int potency) {

    public Strike {
        traits = List.copyOf(traits);
        propertyRunes = List.copyOf(propertyRunes);
    }
}
