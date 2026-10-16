package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CheckIdTest {
    @Test
    void roundTrip() {
        for (String s : new String[] {"skill:athletics", "save:reflex", "perception", "lore:2", "strike:0"}) {
            assertEquals(s, CheckId.parse(s).orElseThrow().format());
        }
    }

    @Test
    void invalid() {
        assertTrue(CheckId.parse("skill:flying").isEmpty());
        assertTrue(CheckId.parse("lore:-1").isEmpty());
        assertTrue(CheckId.parse("").isEmpty());
        assertTrue(CheckId.parse(null).isEmpty());
    }

    @Test
    void labels() {
        CharacterSheet s = Fixtures.fighter5();
        assertEquals("Athletics", CheckId.parse("skill:athletics").get().label(s));
        assertEquals("Reflex", CheckId.parse("save:reflex").get().label(s));
        assertEquals("Perception", CheckId.parse("perception").get().label(s));
        assertEquals("Lore: Farming", CheckId.parse("lore:0").get().label(s));
        assertEquals("Lion Scythe", CheckId.parse("strike:0").get().label(s));
    }
}
