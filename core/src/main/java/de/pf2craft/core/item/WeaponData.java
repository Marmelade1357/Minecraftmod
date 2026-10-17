package de.pf2craft.core.item;

import java.util.List;

/** A weapon from the item catalog. Range is in feet (0 = melee); damageType is B, P or S. */
public record WeaponData(String id, String name, String category, String die, String damageType,
        String group, int hands, int range, List<String> traits) {
}
