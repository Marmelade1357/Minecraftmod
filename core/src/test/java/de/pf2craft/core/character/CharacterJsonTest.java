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
    void damageDiceRoundTripsAndDefaultsToOne() throws Exception {
        String json = java.nio.file.Files.readString(java.nio.file.Paths.get(
                getClass().getResource("/pathbuilder/315329-magus9.json").toURI()));
        CharacterSheet magus = PathbuilderImporter.parse(json);
        assertEquals(magus, CharacterJson.read(CharacterJson.write(magus)));
        String legacy = CharacterJson.write(Fixtures.fighter5()).replace("\"damageDice\":1,", "");
        assertEquals(1, CharacterJson.read(legacy).strikes().get(0).damageDice());
    }

    @Test
    void invalidInputThrows() {
        assertThrows(CharacterImportException.class, () -> CharacterJson.read("{}"));
        assertThrows(CharacterImportException.class, () -> CharacterJson.read("not json"));
    }
}
