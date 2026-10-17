package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void legacySheetJsonStillLoads() throws Exception {
        String json = java.nio.file.Files.readString(java.nio.file.Paths.get(
                getClass().getResource("/pathbuilder/jack-necromancer9.json").toURI()));
        CharacterSheet full = PathbuilderImporter.parse(json,
                de.pf2craft.core.item.TestCatalog.shipped());
        assertTrue(full.shield().isPresent());
        assertEquals(full, CharacterJson.read(CharacterJson.write(full)));

        com.google.gson.JsonObject o = com.google.gson.JsonParser
                .parseString(CharacterJson.write(Fixtures.fighter5())).getAsJsonObject();
        o.remove("shield");
        for (com.google.gson.JsonElement el : o.getAsJsonArray("strikes")) {
            for (String k : new String[] {"group", "traits", "propertyRunes", "potency"}) {
                el.getAsJsonObject().remove(k);
            }
        }
        CharacterSheet legacy = CharacterJson.read(o.toString());
        assertEquals("", legacy.strikes().get(0).group());
        assertTrue(legacy.strikes().get(0).traits().isEmpty());
        assertTrue(legacy.strikes().get(0).propertyRunes().isEmpty());
        assertEquals(0, legacy.strikes().get(0).potency());
        assertTrue(legacy.shield().isEmpty());
    }

    @Test
    void invalidInputThrows() {
        assertThrows(CharacterImportException.class, () -> CharacterJson.read("{}"));
        assertThrows(CharacterImportException.class, () -> CharacterJson.read("not json"));
    }
}
