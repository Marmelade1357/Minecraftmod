package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void strikeDamageBonusMatchesEveryFixture(Path p) throws Exception {
        String json = read(p);
        CharacterSheet s = PathbuilderImporter.parse(json);
        JsonArray weapons = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("build")
                .getAsJsonArray("weapons");
        for (int i = 0; i < weapons.size(); i++) {
            JsonObject w = weapons.get(i).getAsJsonObject();
            assertEquals(w.get("damageBonus").getAsInt(), CharacterMath.strikeDamageBonus(s, i),
                    w.get("name").getAsString());
        }
    }

    @Test
    void otherAttackBonusIsPlausible() throws Exception {
        int total = 0;
        List<String> odd = new java.util.ArrayList<>();
        for (Path p : fixtures()) {
            CharacterSheet s = PathbuilderImporter.parse(read(p));
            for (Strike st : s.strikes()) {
                total++;
                if (st.otherAttackBonus() < 0 || st.otherAttackBonus() > 4) {
                    odd.add(p.getFileName() + ": " + st.name() + " other=" + st.otherAttackBonus());
                }
            }
        }
        assertTrue(odd.size() * 10 <= total, odd.size() + " of " + total + " strikes: " + odd);
        System.out.println("Strikes without a fitting STR/DEX split: " + odd.size() + " of " + total
                + " " + odd);
    }

    private static String build(String level, String abilities, String extra) {
        return "{\"success\":true,\"build\":{\"name\":\"X\",\"class\":\"Fighter\",\"level\":" + level
                + ",\"abilities\":" + abilities + ",\"proficiencies\":{}" + extra + "}}";
    }

    private static final String GOOD_ABILITIES =
            "{\"str\":10,\"dex\":10,\"con\":10,\"int\":10,\"wis\":10,\"cha\":10}";

    private static void assertRejected(String json, String messageFragment) {
        CharacterImportException e = assertThrows(CharacterImportException.class,
                () -> PathbuilderImporter.parse(json), json);
        assertTrue(e.getMessage().contains(messageFragment), e.getMessage());
    }

    @Test
    void rejectsBrokenInput() {
        assertRejected("{\"success\":false}", "reported an error");
        assertRejected("{}", "no build");
        assertRejected("[]", "JSON object");
        assertRejected("not json", "Invalid Pathbuilder data");
        assertRejected(build("0", GOOD_ABILITIES, ""), "Level");
        assertRejected(build("21", GOOD_ABILITIES, ""), "Level");
        assertRejected(build("1", GOOD_ABILITIES, ",\"dualClass\":\"Wizard\""), "Dual class");
        assertRejected(build("1", GOOD_ABILITIES.replace("\"str\":10", "\"str\":0"), ""),
                "out of range");
        assertRejected(build("1", GOOD_ABILITIES.replace("\"str\":10", "\"str\":31"), ""),
                "out of range");
        String weapons = String.join(",", java.util.Collections.nCopies(65, "{\"name\":\"W\"}"));
        assertRejected(build("1", GOOD_ABILITIES, ",\"weapons\":[" + weapons + "]"),
                "Too many entries");
        String sixtyFour = String.join(",", java.util.Collections.nCopies(64, "{\"name\":\"W\"}"));
        assertDoesNotThrow(() ->
                PathbuilderImporter.parse(build("1", GOOD_ABILITIES, ",\"weapons\":[" + sixtyFour + "]")));
    }
}
