package de.pf2craft.core.spell;

import de.pf2craft.core.character.CharacterSheet;

/** Spell attack, spell DC and heightening helpers. */
public final class SpellMath {
    private SpellMath() {
    }

    public static int spellAttack(CharacterSheet sheet, SpellcastingEntry e) {
        return e.rank().bonus(sheet.level()) + sheet.abilityMod(e.ability());
    }

    public static int spellDc(CharacterSheet sheet, SpellcastingEntry e) {
        return 10 + spellAttack(sheet, e);
    }

    /** Highest spell rank a caster of this level can cast, e.g. 1 at level 1-2, 10 at level 19-20. */
    public static int autoHeightenRank(int level) {
        return Math.max(1, (level + 1) / 2);
    }
}
