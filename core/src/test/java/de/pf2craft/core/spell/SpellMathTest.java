package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.ArmorClass;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.HitPoints;
import de.pf2craft.core.character.Rank;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SpellMathTest {
    @Test
    void spellAttackForLevel5ExpertWis18() {
        CharacterSheet sheet = new CharacterSheet("Test", "Cleric", "Human", "", "", 5, Ability.WIS,
                Map.of(Ability.WIS, 18), Map.of(), List.of(), new HitPoints(8, 10, 2, 0), 25,
                new ArmorClass("unarmored", OptionalInt.empty(), 0), List.of(), List.of(), List.of(),
                40, false, Optional.empty());
        SpellcastingEntry e = new SpellcastingEntry("Cleric", "divine", CastingType.PREPARED,
                Ability.WIS, Rank.EXPERT, new int[11], Map.of());
        assertEquals(13, SpellMath.spellAttack(sheet, e));
        assertEquals(23, SpellMath.spellDc(sheet, e));
    }

    @Test
    void autoHeightenRank() {
        assertEquals(1, SpellMath.autoHeightenRank(1));
        assertEquals(1, SpellMath.autoHeightenRank(2));
        assertEquals(2, SpellMath.autoHeightenRank(3));
        assertEquals(10, SpellMath.autoHeightenRank(20));
    }
}
