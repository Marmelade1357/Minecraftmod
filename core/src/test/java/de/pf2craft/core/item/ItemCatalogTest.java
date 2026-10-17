package de.pf2craft.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemCatalogTest {
    private static final String WEAPONS = """
            [
              {"id":"longsword","name":"Longsword","category":"martial","die":"d8","damageType":"S",
               "group":"sword","hands":1,"range":0,"traits":["versatile-p"]},
              {"id":"shortbow","name":"Shortbow","category":"martial","die":"d6","damageType":"P",
               "group":"bow","hands":2,"range":60,"traits":["deadly-d10"]}
            ]""";
    private static final String ARMOR = """
            [{"id":"full-plate","name":"Full Plate","category":"heavy","acBonus":6,"dexCap":0,
              "checkPenalty":3,"speedPenalty":10,"strength":18,"traits":["bulwark"]}]""";
    private static final String SHIELDS = """
            [{"id":"steel-shield","name":"Steel Shield","acBonus":2,"hardness":5,"hitPoints":20,
              "brokenThreshold":10,"speedPenalty":0}]""";

    private static String weapon(String die, String category, String group, String hands, String id) {
        return "[{\"id\":\"" + id + "\",\"name\":\"X\",\"category\":\"" + category + "\",\"die\":\"" + die
                + "\",\"damageType\":\"P\"," + (group == null ? "" : "\"group\":\"" + group + "\",")
                + "\"hands\":" + hands + ",\"range\":0,\"traits\":[]}]";
    }

    @Test
    void parsesAndLooksUpWeapons() {
        ItemCatalog catalog = new ItemCatalog(ItemCatalogParser.weapons(WEAPONS), List.of(), List.of());
        assertTrue(catalog.weapon("Longsword").isPresent());
        assertEquals("d8", catalog.weapon("Longsword").get().die());
        assertTrue(catalog.weapon("+1 Striking Longsword").isPresent());
        assertFalse(catalog.weapon("Lion Scythe").isPresent());
        assertEquals(60, catalog.weapon("shortbow").get().range());
        assertEquals(2, catalog.size());
    }

    @Test
    void normalises() {
        assertEquals("rapier", ItemCatalog.normalise("+2 Greater Striking Rapier"));
        assertEquals("composite longbow", ItemCatalog.normalise("Composite  Longbow"));
    }

    @Test
    void laterEntriesOverride() {
        ItemCatalog catalog = new ItemCatalog(
                ItemCatalogParser.weapons(weapon("d4", "simple", "knife", "1", "dagger")),
                List.of(), List.of());
        ItemCatalog merged = new ItemCatalog(
                List.of(catalog.weapon("x").get(), ItemCatalogParser.weapons(
                        weapon("d6", "simple", "knife", "1", "dagger2")).get(0)), List.of(), List.of());
        assertEquals("d6", merged.weapon("X").get().die());
    }

    @Test
    void rejectsInvalidWeapons() {
        assertRejects(weapon("d7", "martial", "sword", "1", "badDie"), "badDie");
        assertRejects(weapon("d8", "exotic", "sword", "1", "badCat"), "badCat");
        assertRejects(weapon("d8", "martial", null, "1", "noGroup"), "noGroup");
        assertRejects(weapon("d8", "martial", "sword", "-1", "negHands"), "negHands");
        String dup = "[{\"id\":\"dagger\",\"name\":\"D\",\"category\":\"simple\",\"die\":\"d4\","
                + "\"damageType\":\"P\",\"group\":\"knife\",\"hands\":1,\"range\":0,\"traits\":[]},"
                + "{\"id\":\"dagger\",\"name\":\"D\",\"category\":\"simple\",\"die\":\"d4\","
                + "\"damageType\":\"P\",\"group\":\"knife\",\"hands\":1,\"range\":0,\"traits\":[]}]";
        assertRejects(dup, "dagger");
    }

    private static void assertRejects(String json, String id) {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> ItemCatalogParser.weapons(json));
        assertTrue(e.getMessage().contains(id), e.getMessage());
    }

    @Test
    void parsesArmorAndShields() {
        ItemCatalog catalog = new ItemCatalog(List.of(), ItemCatalogParser.armor(ARMOR),
                ItemCatalogParser.shields(SHIELDS));
        assertEquals(6, catalog.armor("Full Plate").get().acBonus());
        assertEquals(5, catalog.shield("Steel Shield").get().hardness());
        assertEquals(2, catalog.size());
    }

    @Test
    void rejectsInvalidArmorAndShields() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ItemCatalogParser.armor(ARMOR.replace("\"heavy\"", "\"plate\"")));
        assertTrue(e.getMessage().contains("full-plate"));
        e = assertThrows(IllegalArgumentException.class,
                () -> ItemCatalogParser.shields(SHIELDS.replace("\"hardness\":5,", "")));
        assertTrue(e.getMessage().contains("steel-shield"));
    }
}
