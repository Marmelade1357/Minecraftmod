package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpellbookJsonTest {
    @Test
    void jsonRoundTripKeepsUsedSlots() throws Exception {
        Spellbook b = PathbuilderSpells.parse(PathbuilderSpellsTest.build("142926-wizard20.json"));
        int w = PathbuilderSpellsTest.indexOf(b, "Wizard");
        b.spendSlot(w, 1);
        b.spendSlot(w, 1);
        b.spendFocus();
        Spellbook r = SpellbookJson.read(SpellbookJson.write(b));
        assertEquals(b.entries().size(), r.entries().size());
        assertEquals(1, r.slotsLeft(w, 1));
        assertEquals(1, r.focusCurrent());
        assertEquals(2, r.focusMax());
        int f = PathbuilderSpellsTest.indexOf(r, "Focus Spells");
        assertTrue(r.entries().get(f).spellsByRank().get(1).contains("Magic's Vessel"));
    }

    @Test
    void garbageJsonGivesEmptyBook() {
        assertTrue(SpellbookJson.read("not json {{").entries().isEmpty());
        assertTrue(SpellbookJson.read("[1,2]").entries().isEmpty());
        assertTrue(SpellbookJson.read("{}").entries().isEmpty());
        assertTrue(SpellbookJson.read(null).entries().isEmpty());
    }
}
