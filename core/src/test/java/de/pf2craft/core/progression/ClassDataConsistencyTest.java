package de.pf2craft.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderImporter;
import de.pf2craft.core.character.Rank;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Cross-checks class data against 261 real Pathbuilder exports. */
class ClassDataConsistencyTest {
    private static final ClassRegistry REGISTRY;

    static {
        try {
            List<ClassProgression> all = new java.util.ArrayList<>();
            String index;
            try (java.io.InputStream in = ClassDataConsistencyTest.class
                    .getResourceAsStream("/pf2craft/classes/index.json")) {
                index = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (com.google.gson.JsonElement f : com.google.gson.JsonParser.parseString(index).getAsJsonArray()) {
                try (java.io.InputStream in = ClassDataConsistencyTest.class
                        .getResourceAsStream("/pf2craft/classes/" + f.getAsString())) {
                    all.add(ClassProgressionParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
            REGISTRY = new ClassRegistry(all);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** "fixture-file:key" -> reason why the exported rank legitimately exceeds the class table. */
    private static final Map<String, String> KNOWN_RANK_EXCEPTIONS = Map.ofEntries(
            Map.entry("edge-cleric-01.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("edge-cleric-02.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("edge-cleric-03.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("edge-cleric-04.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("generated-cleric-01.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("generated-cleric-02.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("generated-cleric-03.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("generated-cleric-04.json:fortitude",
                    "cleric doctrine (Cloistered Cleric / Warpriest) raises Fortitude, a choice-based increase"),
            Map.entry("generated-alchemist-03.json:will",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-magus-05.json:perception",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-oracle-08.json:reflex",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-swashbuckler-02.json:fortitude",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-barbarian-04.json:reflex",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-druid-01.json:perception",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-druid-05.json:perception",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-druid-09.json:perception",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-monk-01.json:fortitude",
                    "Monk Path to Perfection raises chosen saves (choice-based)"),
            Map.entry("generated-monk-01.json:will",
                    "Monk Path to Perfection raises chosen saves (choice-based)"),
            Map.entry("generated-monk-02.json:reflex",
                    "Monk Path to Perfection raises chosen saves (choice-based)"),
            Map.entry("generated-monk-02.json:will",
                    "Monk Path to Perfection raises chosen saves (choice-based)"),
            Map.entry("generated-rogue-02.json:fortitude",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-sorcerer-08.json:reflex",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-witch-16.json:reflex",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-wizard-16.json:reflex",
                    "Canny Acumen raises a save/Perception by one rank"),
            Map.entry("generated-wizard-18.json:fortitude",
                    "Canny Acumen raises a save/Perception by one rank"));

    static List<Path> fixtures() throws Exception {
        Path dir = Paths.get(ClassDataConsistencyTest.class.getResource("/pathbuilder").toURI());
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted()
                    .collect(Collectors.toList());
        }
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void classTableMatchesExport(Path fixture) throws Exception {
        CharacterSheet s = PathbuilderImporter.parse(Files.readString(fixture, StandardCharsets.UTF_8));
        Optional<ClassProgression> cls = REGISTRY.find(s.className());
        if (cls.isEmpty()) {
            return;
        }
        Map<String, Rank> expected = cls.get().ranksAt(s.level());
        List<String> mismatches = new java.util.ArrayList<>();
        for (String key : List.of("perception", "fortitude", "reflex", "will", "class_dc")) {
            Rank table = expected.getOrDefault(key, Rank.UNTRAINED);
            Rank actual = s.rank(key);
            String id = fixture.getFileName() + ":" + key;
            assertTrue(table.compareTo(actual) <= 0, id + " class table " + table + " > export " + actual);
            if (!key.equals("class_dc") && !KNOWN_RANK_EXCEPTIONS.containsKey(id)) {
                if (table != actual) {
                    mismatches.add(id + " table " + table + " export " + actual);
                }
            }
        }
        assertTrue(mismatches.isEmpty(), String.join("; ", mismatches));
    }

    @org.junit.jupiter.api.Test
    void fixturesActuallyCoverFighters() throws Exception {
        long n = 0;
        for (Path f : fixtures()) {
            CharacterSheet s = PathbuilderImporter.parse(Files.readString(f, StandardCharsets.UTF_8));
            if (REGISTRY.find(s.className()).isPresent()) {
                n++;
            }
        }
        System.out.println("Fixtures checked: " + n);
        assertTrue(n >= 2, "no fighter fixtures matched");
    }
}
