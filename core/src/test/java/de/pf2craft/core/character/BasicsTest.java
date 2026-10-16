package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BasicsTest {
    @Test
    void abilityModifier() {
        assertEquals(4, Ability.modifier(18));
        assertEquals(0, Ability.modifier(10));
        assertEquals(-1, Ability.modifier(9));
    }

    @Test
    void rankBonus() {
        assertEquals(0, Rank.UNTRAINED.bonus(5));
        assertEquals(7, Rank.TRAINED.bonus(5));
        assertEquals(9, Rank.EXPERT.bonus(5));
        assertEquals(13, Rank.LEGENDARY.bonus(5));
        assertEquals(Rank.EXPERT, Rank.fromPathbuilder(4));
        assertThrows(IllegalArgumentException.class, () -> Rank.fromPathbuilder(3));
        assertEquals(Rank.MASTER, Rank.EXPERT.next());
    }

    @Test
    void skillsAndSaves() {
        assertEquals(16, Skill.values().length);
        assertEquals(Ability.STR, Skill.ATHLETICS.ability());
        assertEquals("athletics", Skill.ATHLETICS.id());
        assertEquals("Athletics", Skill.ATHLETICS.displayName());
        assertEquals(Ability.WIS, Save.WILL.ability());
        assertEquals("fortitude", Save.FORTITUDE.id());
    }

    @Test
    void abilityFromPathbuilder() {
        assertEquals(Ability.DEX, Ability.fromPathbuilder("Dex"));
        assertEquals(Ability.STR, Ability.fromPathbuilder("str"));
        assertEquals("cha", Ability.CHA.pathbuilderKey());
    }
}
