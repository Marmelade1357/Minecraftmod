package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
