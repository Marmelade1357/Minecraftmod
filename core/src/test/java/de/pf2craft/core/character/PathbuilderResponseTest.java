package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.item.TestCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class PathbuilderResponseTest {
    @Test
    void nonOkStatusFails() {
        CharacterImportException e = assertThrows(CharacterImportException.class,
                () -> PathbuilderResponse.toSheet(500, "{}"));
        assertTrue(e.getMessage().contains("status 500"));
    }

    @Test
    void garbageFails() {
        assertThrows(CharacterImportException.class, () -> PathbuilderResponse.toSheet(200, "<html>"));
    }

    @Test
    void hugeBodyFails() {
        String big = "x".repeat(3 * 1024 * 1024);
        assertThrows(CharacterImportException.class, () -> PathbuilderResponse.toSheet(200, big));
    }

    @Test
    void fixtureParses() throws Exception {
        String body = Files.readString(Paths.get(getClass().getResource("/pathbuilder/422753-fighter5.json").toURI()),
                StandardCharsets.UTF_8);
        assertEquals("August [Kholo, F]", PathbuilderResponse.toSheet(200, body).name());
    }

    @Test
    void fixtureParsesWithCatalog() throws Exception {
        String body = Files.readString(Paths.get(getClass().getResource("/pathbuilder/422753-fighter5.json").toURI()),
                StandardCharsets.UTF_8);
        CharacterSheet sheet = PathbuilderResponse.toSheet(200, body, TestCatalog.shipped());
        assertEquals("Lion Scythe", sheet.strikes().get(0).name());
        assertEquals("d6", sheet.strikes().get(0).damageDie());
    }
}
