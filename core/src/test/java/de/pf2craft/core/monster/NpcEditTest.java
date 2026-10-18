package de.pf2craft.core.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.progression.InvalidChoiceException;
import org.junit.jupiter.api.Test;

class NpcEditTest {
    private static NpcStats fresh() {
        return NpcStats.fresh(NpcTestData.kobold());
    }

    @Test
    void freshUsesBaseNameAndHp() {
        NpcStats s = fresh();
        assertEquals("Kobold Warrior", s.displayName());
        assertEquals(8, s.currentHp());
        assertFalse(s.defeated());
    }

    @Test
    void hpIsClampedAndDefeatedTracksZero() throws InvalidChoiceException {
        assertEquals(8, NpcEdit.apply(fresh(), "hp", "999").currentHp());
        NpcStats down = NpcEdit.apply(fresh(), "hp", "-5");
        assertEquals(0, down.currentHp());
        assertTrue(down.defeated());
        assertFalse(NpcEdit.apply(down, "hp", "3").defeated());
    }

    @Test
    void maxHpShrinkClampsCurrent() throws InvalidChoiceException {
        NpcStats s = NpcEdit.apply(fresh(), "maxhp", "5");
        assertEquals(5, s.base().hp());
        assertEquals(5, s.currentHp());
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "maxhp", "0"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "maxhp", "10000"));
    }

    @Test
    void statEdits() throws InvalidChoiceException {
        assertEquals(25, NpcEdit.apply(fresh(), "ac", "25").base().ac());
        assertEquals(9, NpcEdit.apply(fresh(), "fortitude", "9").base().fortitude());
        assertEquals(9, NpcEdit.apply(fresh(), "reflex", "9").base().reflex());
        assertEquals(9, NpcEdit.apply(fresh(), "will", "9").base().will());
        assertEquals(-10, NpcEdit.apply(fresh(), "perception", "-10").base().perception());
        assertEquals(30, NpcEdit.apply(fresh(), "speed", "30").base().speed());
        assertEquals(3, NpcEdit.apply(fresh(), "level", "3").base().level());
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "perception", "100"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "level", "26"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "level", "-2"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "speed", "301"));
    }

    @Test
    void speedMustBeMultipleOfFive() {
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "speed", "27"));
    }

    @Test
    void nameValidation() throws InvalidChoiceException {
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "name", "\u00a7cX"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "name", ""));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "name", "x".repeat(33)));
        assertEquals("Kobold 1", NpcEdit.apply(fresh(), "name", "Kobold 1").displayName());
    }

    @Test
    void strikeBonus() throws InvalidChoiceException {
        NpcStats s = NpcEdit.apply(fresh(), "strike:0:bonus", "9");
        assertEquals(9, s.base().strikes().get(0).attackBonus());
        assertEquals("Spear", s.base().strikes().get(0).name());
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "strike:7:bonus", "9"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "strike:x:bonus", "9"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "strike:0:bonus", "100"));
    }

    @Test
    void unknownFieldAndBadNumberAreRejected() {
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "mana", "3"));
        assertThrows(InvalidChoiceException.class, () -> NpcEdit.apply(fresh(), "ac", "abc"));
    }
}
