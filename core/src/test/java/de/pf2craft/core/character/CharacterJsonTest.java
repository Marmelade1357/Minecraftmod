package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CharacterJsonTest {
    @Test
    void roundTrip() throws Exception {
        CharacterSheet s = Fixtures.fighter5();
        assertEquals(s, CharacterJson.read(CharacterJson.write(s)));
    }

    @Test
    void invalidInputThrows() {
        assertThrows(CharacterImportException.class, () -> CharacterJson.read("{}"));
        assertThrows(CharacterImportException.class, () -> CharacterJson.read("not json"));
    }
}
