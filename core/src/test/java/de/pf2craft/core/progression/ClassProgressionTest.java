package de.pf2craft.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.character.Rank;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassProgressionTest {
    static ClassProgression fighter() throws IOException {
        try (InputStream in = ClassProgressionTest.class.getResourceAsStream("/pf2craft/classes/fighter.json")) {
            return ClassProgressionParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void parsesFighter() throws IOException {
        ClassProgression f = fighter();
        assertEquals("Fighter", f.name());
        assertEquals(10, f.hpPerLevel());
        assertEquals(20, f.levels().size());
    }

    @Test
    void ranksAccumulateAndHighestWins() throws IOException {
        ClassProgression f = fighter();
        assertEquals(Rank.EXPERT, f.ranksAt(1).get("fortitude"));
        assertEquals(Rank.EXPERT, f.ranksAt(6).get("perception"));
        assertEquals(Rank.MASTER, f.ranksAt(7).get("perception"));
        assertEquals(Rank.EXPERT, f.ranksAt(3).get("will"));
        assertEquals(Rank.MASTER, f.ranksAt(20).get("class_dc"));
        assertEquals(Rank.TRAINED, f.ranksAt(10).get("class_dc"));
        assertEquals(Rank.EXPERT, f.ranksAt(13).get("advanced"));
    }

    @Test
    void levelEntries() throws IOException {
        ClassProgression f = fighter();
        assertTrue(f.levels().get(3).skillIncrease());
        assertFalse(f.levels().get(4).skillIncrease());
        assertEquals(List.of("Class Feat", "Skill Feat"), f.levels().get(2).featSlots());
        assertEquals(List.of("Class Feat"), f.levels().get(1).featSlots());
    }

    @Test
    void sharedProgressionConstants() {
        assertEquals(List.of(1, 5, 9, 13, 17), ClassProgression.ANCESTRY_FEAT_LEVELS);
        assertEquals(List.of(3, 7, 11, 15, 19), ClassProgression.GENERAL_FEAT_LEVELS);
        assertEquals(List.of(5, 10, 15, 20), ClassProgression.ABILITY_BOOST_LEVELS);
    }

    @Test
    void registryFindsCaseInsensitive() throws IOException {
        ClassRegistry r = new ClassRegistry(List.of(fighter()));
        assertTrue(r.find("fighter").isPresent());
        assertTrue(r.find("FIGHTER").isPresent());
        assertTrue(r.find("Wizard").isEmpty());
        assertEquals(1, r.size());
    }

    @Test
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ClassProgressionParser.parse("{\"name\":\"X\"}"));
        assertThrows(IllegalArgumentException.class, () -> ClassProgressionParser.parse("nope"));
        assertThrows(IllegalArgumentException.class, () -> ClassProgressionParser.parse(
                "{\"name\":\"X\",\"hpPerLevel\":8,\"levels\":{\"1\":{\"proficiencies\":{\"will\":\"bogus\"}}}}"));
    }
}
