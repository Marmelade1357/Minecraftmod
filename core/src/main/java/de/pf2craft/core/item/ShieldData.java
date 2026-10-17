package de.pf2craft.core.item;

/** A shield from the item catalog. */
public record ShieldData(String id, String name, int acBonus, int hardness, int hitPoints,
        int brokenThreshold, int speedPenalty) {
}
