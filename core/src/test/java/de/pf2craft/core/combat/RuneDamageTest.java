package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuneDamageTest {
    @Test
    void gradeWordsAreStripped() {
        for (String rune : List.of("flaming", "Greater Flaming", "major flaming", "True Flaming",
                "Flaming (Greater)", "greater major flaming")) {
            assertEquals(List.of(new DamageDice("1d6", 0, "fire")), RuneDamage.parts(List.of(rune)), rune);
        }
    }

    @Test
    void unknownRunesAddNothing() {
        assertEquals(List.of(), RuneDamage.parts(List.of("greater reinforcing", "ghost touch")));
    }
}
