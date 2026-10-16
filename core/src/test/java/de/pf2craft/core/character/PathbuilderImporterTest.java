package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PathbuilderImporterTest {
    /** Fixtures whose AC differs from Pathbuilder for a reason the model ignores on purpose. */
    private static final Set<String> KNOWN_AC_EXCEPTIONS = Set.of();

    static List<Path> fixtures() throws IOException, URISyntaxException {
        Path dir = Paths.get(PathbuilderImporterTest.class.getResource("/pathbuilder").toURI());
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void fighter5MatchesPathbuilder() throws Exception {
        Path p = Paths.get(getClass().getResource("/pathbuilder/422753-fighter5.json").toURI());
        CharacterSheet s = PathbuilderImporter.parse(read(p));
        assertEquals(75, CharacterMath.maxHp(s));
        assertEquals(23, CharacterMath.armorClass(s));
        assertEquals(13, CharacterMath.skill(s, Skill.ATHLETICS));
        assertEquals(9, CharacterMath.save(s, Save.REFLEX));
        assertEquals(12, CharacterMath.perception(s));
        int farming = -1;
        for (int i = 0; i < s.lores().size(); i++) {
            if (s.lores().get(i).name().equals("Farming")) {
                farming = i;
            }
        }
        assertEquals(0, farming);
        assertEquals(Rank.TRAINED, s.lores().get(farming).rank());
        assertEquals(75, s.currentHp());
        assertFalse(s.levelUpGranted());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void armorClassMatchesEveryFixture(Path p) throws Exception {
        if (KNOWN_AC_EXCEPTIONS.contains(p.getFileName().toString())) {
            return;
        }
        String json = read(p);
        CharacterSheet s = PathbuilderImporter.parse(json);
        int expected = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("build")
                .getAsJsonObject("acTotal").get("acTotal").getAsInt();
        assertEquals(expected, CharacterMath.armorClass(s));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void strikeAttackMatchesEveryFixture(Path p) throws Exception {
        String json = read(p);
        CharacterSheet s = PathbuilderImporter.parse(json);
        JsonArray weapons = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("build")
                .getAsJsonArray("weapons");
        assertEquals(weapons.size(), s.strikes().size());
        for (int i = 0; i < weapons.size(); i++) {
            JsonObject w = weapons.get(i).getAsJsonObject();
            assertEquals(w.get("attack").getAsInt(), CharacterMath.strikeAttack(s, i),
                    w.get("name").getAsString());
        }
    }

    @Test
    void rejectsBrokenInput() {
        String[] bad = {
            "{\"success\":false}", "{}", "[]", "not json",
            "{\"success\":true,\"build\":{\"name\":\"X\",\"class\":\"Fighter\",\"level\":0,"
                + "\"abilities\":{},\"proficiencies\":{}}}",
            "{\"success\":true,\"build\":{\"name\":\"X\",\"class\":\"Fighter\",\"level\":1,"
                + "\"dualClass\":\"Wizard\",\"abilities\":{},\"proficiencies\":{}}}",
        };
        for (String json : bad) {
            assertThrows(CharacterImportException.class, () -> PathbuilderImporter.parse(json), json);
        }
    }
}
