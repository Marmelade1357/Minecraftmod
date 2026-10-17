package de.pf2craft.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Checks the shipped item data files and that they cover the items used by real characters. */
class ItemDataConsistencyTest {
    /** Normalised names that appear in the fixtures but are not in the Player Core data. */
    private static final Map<String, String> KNOWN_MISSING = Map.ofEntries(
            Map.entry("a:leaf weave", "Treasure Vault armor, not in Player Core"),
            Map.entry("w:axe musket melee", "Guns & Gears weapon (Axe Musket, melee mode), not in Player Core"),
            Map.entry("w:coat pistol", "Guns and Gears weapon, not in Player Core"),
            Map.entry("w:corset knife", "Treasure Vault weapon, not in Player Core"),
            Map.entry("w:lion scythe", "Battlecry! weapon, not in Player Core"),
            Map.entry("w:special unarmed foxfire", "Special-purpose unarmed attack, not a catalog weapon"));

    private static String resource(String name) throws IOException {
        try (InputStream in = ItemDataConsistencyTest.class.getResourceAsStream("/pf2craft/items/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ItemCatalog catalog() throws IOException {
        return new ItemCatalog(ItemCatalogParser.weapons(resource("weapons.json")),
                ItemCatalogParser.armor(resource("armor.json")),
                ItemCatalogParser.shields(resource("shields.json")));
    }

    @Test
    void filesParseAndHaveEnoughEntries() throws IOException {
        assertTrue(ItemCatalogParser.weapons(resource("weapons.json")).size() >= 60);
        assertTrue(ItemCatalogParser.armor(resource("armor.json")).size() >= 10);
        assertTrue(ItemCatalogParser.shields(resource("shields.json")).size() >= 4);
    }

    @Test
    void weaponSpotValues() throws IOException {
        ItemCatalog c = catalog();
        WeaponData longsword = c.weapon("Longsword").orElseThrow();
        assertEquals("d8", longsword.die());
        assertEquals("S", longsword.damageType());
        assertEquals("martial", longsword.category());
        assertEquals("sword", longsword.group());
        assertTrue(longsword.traits().contains("versatile-p"));

        WeaponData dagger = c.weapon("Dagger").orElseThrow();
        assertEquals("d4", dagger.die());
        assertEquals("P", dagger.damageType());
        assertEquals("simple", dagger.category());
        assertEquals("knife", dagger.group());
        assertTrue(dagger.traits().containsAll(List.of("agile", "finesse", "thrown-10", "versatile-s")));

        WeaponData greatsword = c.weapon("Greatsword").orElseThrow();
        assertEquals("d12", greatsword.die());
        assertEquals("S", greatsword.damageType());
        assertEquals("martial", greatsword.category());
        assertEquals("sword", greatsword.group());
        assertEquals(2, greatsword.hands());

        WeaponData longbow = c.weapon("Longbow").orElseThrow();
        assertEquals("d8", longbow.die());
        assertEquals("P", longbow.damageType());
        assertEquals("martial", longbow.category());
        assertEquals("bow", longbow.group());
        assertEquals(100, longbow.range());
        assertTrue(longbow.traits().containsAll(List.of("deadly-d10", "volley-30")));
    }

    @Test
    void armorAndShieldSpotValues() throws IOException {
        ItemCatalog c = catalog();
        ArmorData plate = c.armor("Full Plate").orElseThrow();
        assertEquals("heavy", plate.category());
        assertEquals(6, plate.acBonus());
        assertEquals(0, plate.dexCap());
        assertEquals(4, plate.strength());

        ArmorData leather = c.armor("Leather Armor").orElseThrow();
        assertEquals("light", leather.category());
        assertEquals(1, leather.acBonus());
        assertEquals(4, leather.dexCap());

        ShieldData steel = c.shield("Steel Shield").orElseThrow();
        assertEquals(2, steel.acBonus());
        assertEquals(5, steel.hardness());
        assertEquals(20, steel.hitPoints());
        assertEquals(10, steel.brokenThreshold());
    }

    @Test
    void fixtureItemsAreInTheCatalog() throws IOException, URISyntaxException {
        ItemCatalog c = catalog();
        Set<String> names = new TreeSet<>();
        Set<String> missing = new TreeSet<>();
        Path dir = Paths.get(getClass().getResource("/pathbuilder").toURI());
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        for (Path file : files) {
            JsonObject build = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("build");
            for (JsonElement el : array(build, "weapons")) {
                String name = el.getAsJsonObject().get("name").getAsString();
                String key = ItemCatalog.normalise(name);
                names.add("w:" + key);
                if (c.weapon(name).isEmpty()) {
                    missing.add("w:" + key);
                }
            }
            for (JsonElement el : array(build, "armor")) {
                JsonObject o = el.getAsJsonObject();
                String name = o.get("name").getAsString();
                String key = ItemCatalog.normalise(name);
                boolean shield = "shield".equals(o.get("prof").getAsString());
                names.add((shield ? "s:" : "a:") + key);
                boolean found = shield ? c.shield(name).isPresent() : c.armor(name).isPresent();
                if (!found) {
                    missing.add((shield ? "s:" : "a:") + key);
                }
            }
        }
        List<String> unexplained = new ArrayList<>();
        for (String m : missing) {
            if (!KNOWN_MISSING.containsKey(m)) {
                unexplained.add(m);
            }
        }
        assertTrue(unexplained.isEmpty(), "Items missing from the catalog: " + unexplained);
        // The fixtures only contain about 15 distinct names, so a percentage cap is meaningless;
        // cap the list absolutely instead.
        assertTrue(names.size() >= 10, "Fixtures should contain item names");
        assertTrue(KNOWN_MISSING.size() <= 6, "KNOWN_MISSING grew beyond 6 entries");
        for (String k : KNOWN_MISSING.keySet()) {
            assertTrue(missing.contains(k), "KNOWN_MISSING entry is no longer missing: " + k);
        }
    }

    private static Iterable<JsonElement> array(JsonObject build, String field) {
        JsonElement el = build.get(field);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : List.of();
    }
}
