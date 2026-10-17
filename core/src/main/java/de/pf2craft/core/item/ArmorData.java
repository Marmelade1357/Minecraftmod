package de.pf2craft.core.item;

import java.util.List;

/** An armor from the item catalog. */
public record ArmorData(String id, String name, String category, int acBonus, int dexCap,
        int checkPenalty, int speedPenalty, int strength, List<String> traits) {
}
