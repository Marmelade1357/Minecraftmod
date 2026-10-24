package de.pf2craft.core.spell;

/**
 * One spell of the catalog (numbers only). Rank 0 is a cantrip. rangeFeet 0 means touch or self,
 * maxTargets 0 means self, 1 a single target, more an area or multi-target spell.
 * save is fortitude, reflex, will or empty. damageDice looks like "2d4" or is empty.
 * heightenStep is the number of ranks per heighten step (1 or 2), 0 for none.
 */
public record SpellDefinition(String id, String name, int rank, int actions, int rangeFeet, int maxTargets,
        SpellKind kind, String save, boolean basicSave, String damageDice, int damageBonus, String damageType,
        String heightenDice, int heightenStep, boolean addAbilityMod) {
}
