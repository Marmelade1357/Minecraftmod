package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.Rank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpellbookTest {
    private static Spellbook book() {
        int[] days = {5, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0};
        SpellcastingEntry prepared = new SpellcastingEntry("Wizard", "arcane", CastingType.PREPARED,
                Ability.INT, Rank.TRAINED, days, Map.of());
        SpellcastingEntry focus = new SpellcastingEntry("Focus Spells", "focus", CastingType.FOCUS,
                Ability.INT, Rank.TRAINED, new int[11], Map.of());
        return new Spellbook(List.of(prepared, focus), 2);
    }

    @Test
    void focusAndInnateEntriesHaveNoSlotsAboveCantrips() {
        SpellcastingEntry focus = new SpellcastingEntry("Focus Spells", "focus", CastingType.FOCUS,
                Ability.INT, Rank.TRAINED, new int[] {0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0}, Map.of());
        Spellbook b = new Spellbook(List.of(focus), 1);
        assertEquals(0, b.slotsLeft(0, 1));
        assertEquals(Integer.MAX_VALUE, b.slotsLeft(0, 0));
    }

    @Test
    void cantripsAreFree() {
        assertEquals(Integer.MAX_VALUE, book().slotsLeft(0, 0));
    }

    @Test
    void spendSlotCountsDown() {
        Spellbook b = book();
        assertEquals(2, b.slotsLeft(0, 1));
        assertTrue(b.spendSlot(0, 1));
        assertEquals(1, b.slotsLeft(0, 1));
    }

    @Test
    void spendSlotRefusedWhenEmpty() {
        Spellbook b = book();
        assertTrue(b.spendSlot(0, 2));
        assertFalse(b.spendSlot(0, 2));
        assertEquals(0, b.slotsLeft(0, 2));
        assertFalse(b.spendSlot(0, 3));
        assertFalse(b.spendSlot(1, 1));
    }

    @Test
    void restRefillsSlotsAndFocus() {
        Spellbook b = book();
        b.spendSlot(0, 1);
        b.spendFocus();
        assertEquals(1, b.focusCurrent());
        b.rest();
        assertEquals(2, b.slotsLeft(0, 1));
        assertEquals(2, b.focusCurrent());
    }

    @Test
    void focusCannotGoBelowZeroAndCopyIsIndependent() {
        Spellbook b = book();
        Spellbook c = b.copy();
        assertTrue(b.spendFocus());
        assertTrue(b.spendFocus());
        assertFalse(b.spendFocus());
        assertEquals(2, c.focusCurrent());
    }
}
