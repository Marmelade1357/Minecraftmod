package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.pf2craft.core.progression.InvalidChoiceException;
import org.junit.jupiter.api.Test;

class GmEditTest {
    private final CharacterSheet s = Fixtures.fighter5();

    @Test
    void hpIsClampedToZeroAndMax() throws Exception {
        assertEquals(75, GmEdit.apply(s, "hp", 200).currentHp());
        assertEquals(0, GmEdit.apply(s, "hp", -5).currentHp());
        assertEquals(40, GmEdit.apply(s, "hp", 40).currentHp());
    }

    @Test
    void abilityScore() throws Exception {
        assertEquals(20, GmEdit.apply(s, "ability:str", 20).abilityScores().get(Ability.STR));
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "ability:str", 31));
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "ability:str", 0));
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "ability:luck", 10));
    }

    @Test
    void rank() throws Exception {
        assertEquals(Rank.MASTER, GmEdit.apply(s, "rank:athletics", 3).rank("athletics"));
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "rank:flying", 1));
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "rank:athletics", 5));
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "rank:athletics", -1));
    }

    @Test
    void unknownField() {
        assertThrows(InvalidChoiceException.class, () -> GmEdit.apply(s, "mana", 3));
    }
}
