package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CharacterMathTest {
    private final CharacterSheet s = Fixtures.fighter5();

    @Test
    void derivedValues() {
        assertEquals(75, CharacterMath.maxHp(s));
        assertEquals(23, CharacterMath.armorClass(s));
        assertEquals(12, CharacterMath.save(s, Save.FORTITUDE));
        assertEquals(9, CharacterMath.save(s, Save.REFLEX));
        assertEquals(12, CharacterMath.save(s, Save.WILL));
        assertEquals(12, CharacterMath.perception(s));
        assertEquals(13, CharacterMath.skill(s, Skill.ATHLETICS));
        assertEquals(7, CharacterMath.skill(s, Skill.ACROBATICS));
        assertEquals(0, CharacterMath.skill(s, Skill.DECEPTION));
        assertEquals(13, CharacterMath.strikeAttack(s, 0));
        assertEquals(4, CharacterMath.strikeDamageBonus(s, 0));
        assertEquals(5, CharacterMath.speedBlocks(s));
    }

    @Test
    void loreAndClassDc() {
        assertEquals(3 + 7, CharacterMath.lore(s, 0));
        assertEquals(10 + 4 + 7, CharacterMath.classDc(s));
    }

    @Test
    void checkBonus() {
        assertEquals(13, CharacterMath.checkBonus(s, CheckId.parse("skill:athletics").get()).getAsInt());
        assertEquals(12, CharacterMath.checkBonus(s, CheckId.parse("perception").get()).getAsInt());
        assertEquals(true, CharacterMath.checkBonus(s, CheckId.parse("strike:5").get()).isEmpty());
        assertEquals(true, CharacterMath.checkBonus(s, CheckId.parse("lore:3").get()).isEmpty());
    }
}
