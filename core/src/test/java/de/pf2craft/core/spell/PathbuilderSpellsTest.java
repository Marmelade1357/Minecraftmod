package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.Rank;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PathbuilderSpellsTest {
    static JsonObject build(String file) throws Exception {
        Path p = Paths.get(PathbuilderSpellsTest.class.getResource("/pathbuilder/" + file).toURI());
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("build");
    }

    static int indexOf(Spellbook book, String name) {
        for (int i = 0; i < book.entries().size(); i++) {
            if (book.entries().get(i).name().equals(name)) {
                return i;
            }
        }
        throw new AssertionError("no entry " + name);
    }

    @Test
    void wizardFixtureHasWizardEntryWithTenRanksOfSlots() throws Exception {
        Spellbook book = PathbuilderSpells.parse(build("142926-wizard20.json"));
        SpellcastingEntry e = book.entries().get(indexOf(book, "Wizard"));
        assertEquals(CastingType.PREPARED, e.type());
        assertEquals(Ability.INT, e.ability());
        assertEquals(Rank.LEGENDARY, e.rank());
        assertEquals(2, e.perDay()[10]);
        assertEquals(3, e.perDay()[1]);
    }

    @Test
    void focusEntryIsFocusType() throws Exception {
        Spellbook book = PathbuilderSpells.parse(build("142926-wizard20.json"));
        SpellcastingEntry e = book.entries().get(indexOf(book, "Focus Spells"));
        assertEquals(CastingType.FOCUS, e.type());
        assertTrue(e.spellsByRank().get(1).contains("Magic's Vessel"));
        assertEquals(2, book.focusMax());
        assertEquals(2, book.focusCurrent());
    }

    @Test
    void missingSpellCastersGivesEmptyBook() {
        assertTrue(PathbuilderSpells.parse(new JsonObject()).entries().isEmpty());
        assertEquals(0, PathbuilderSpells.parse(new JsonObject()).focusMax());
    }

    @Test
    void allFixturesParseWithoutException() throws Exception {
        Path dir = Paths.get(PathbuilderSpellsTest.class.getResource("/pathbuilder").toURI());
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".json")).collect(Collectors.toList());
        }
        assertTrue(files.size() > 100);
        for (Path f : files) {
            JsonObject b = build(f.getFileName().toString());
            Spellbook book = assertDoesNotThrow(() -> PathbuilderSpells.parse(b), f.toString());
            assertNotNull(book);
        }
    }

    @Test
    void parseResponseToleratesGarbage() {
        assertTrue(PathbuilderSpells.parseResponse("not json").entries().isEmpty());
        assertTrue(PathbuilderSpells.parseResponse("{\"success\":true}").entries().isEmpty());
        assertTrue(PathbuilderSpells.parseResponse("{\"build\":{}}").entries().isEmpty());
    }
}
