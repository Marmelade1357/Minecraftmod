package de.pf2craft.core.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonsterCatalogTest {
    private static final String KOBOLD_WARRIOR = """
            {"id":"kobold-warrior","name":"Kobold Warrior","level":-1,"size":"small",
             "traits":["humanoid","kobold"],"perception":2,"ac":16,"fortitude":5,"reflex":7,"will":3,
             "hp":8,"speed":25,
             "strikes":[{"name":"Spear","attackBonus":7,"ranged":false,"traits":["thrown 20 ft."],
               "damage":[{"dice":"1d6","bonus":1,"type":"piercing"},{"dice":"","bonus":1,"type":"fire"}]}],
             "resistances":[],
             "weaknesses":[{"type":"cold","value":2}],
             "immunities":["paralyzed"],
             "skills":{"Acrobatics":5,"Stealth":5},
             "abilities":["Dragon Lore"]}""";

    private static String monster(String id, String name, int level) {
        return """
                {"id":"%s","name":"%s","level":%d,"size":"medium","traits":[],"perception":0,"ac":10,
                 "fortitude":0,"reflex":0,"will":0,"hp":10,"speed":25,"strikes":[],"resistances":[],
                 "weaknesses":[],"immunities":[],"skills":{},"abilities":[]}""".formatted(id, name, level);
    }

    private static String json() {
        return "[" + KOBOLD_WARRIOR + "," + monster("kobold-scout", "Kobold Scout", 1) + ","
                + monster("goblin-warrior", "Goblin Warrior", -1) + "]";
    }

    private static MonsterCatalog catalog() {
        return new MonsterCatalog(MonsterParser.parse(json()));
    }

    private static List<String> ids(List<MonsterData> list) {
        return list.stream().map(MonsterData::id).toList();
    }

    @Test
    void parsesFullEntry() {
        MonsterData m = catalog().byId("kobold-warrior").orElseThrow();
        assertEquals("Kobold Warrior", m.name());
        assertEquals(-1, m.level());
        assertEquals("small", m.size());
        assertEquals(List.of("humanoid", "kobold"), m.traits());
        assertEquals(2, m.perception());
        assertEquals(16, m.ac());
        assertEquals(5, m.fortitude());
        assertEquals(7, m.reflex());
        assertEquals(3, m.will());
        assertEquals(8, m.hp());
        assertEquals(25, m.speed());
        assertEquals(1, m.strikes().size());
        MonsterStrike s = m.strikes().get(0);
        assertEquals("Spear", s.name());
        assertEquals(7, s.attackBonus());
        assertFalse(s.ranged());
        assertEquals(List.of("thrown 20 ft."), s.traits());
        assertEquals(List.of(new DamagePart("1d6", 1, "piercing"), new DamagePart("", 1, "fire")),
                s.damage());
        assertTrue(m.resistances().isEmpty());
        assertEquals(List.of(new Resistance("cold", 2)), m.weaknesses());
        assertEquals(List.of("paralyzed"), m.immunities());
        assertEquals(Map.of("Acrobatics", 5, "Stealth", 5), m.skills());
        assertEquals(List.of("Dragon Lore"), m.abilities());
        assertEquals(3, catalog().size());
    }

    @Test
    void searchIsCaseInsensitiveAndPartial() {
        MonsterCatalog c = catalog();
        assertEquals(List.of("kobold-warrior", "kobold-scout"), ids(c.search("kobold", 10)));
        assertEquals(List.of("kobold-warrior", "kobold-scout"), ids(c.search("KOBOLD", 10)));
        assertEquals(List.of("goblin-warrior", "kobold-warrior"), ids(c.search("warr", 10)));
        assertEquals(List.of("kobold-warrior"), ids(c.search("kobold warrior", 10)));
        assertEquals(2, c.search("", 2).size());
    }

    @Test
    void laterEntriesReplaceById() {
        String second = KOBOLD_WARRIOR.replace("\"hp\":8", "\"hp\":99");
        List<MonsterData> all = new java.util.ArrayList<>(MonsterParser.parse(json()));
        all.addAll(MonsterParser.parse("[" + second + "]"));
        MonsterCatalog c = new MonsterCatalog(all);
        assertEquals(99, c.byId("kobold-warrior").orElseThrow().hp());
        assertEquals(3, c.size());
    }

    private static void assertRejected(String broken, String needle) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MonsterParser.parse("[" + broken + "]"));
        assertTrue(e.getMessage().contains("kobold-warrior"), e.getMessage());
        assertTrue(e.getMessage().contains(needle), e.getMessage());
    }

    @Test
    void rejectsBrokenEntries() {
        assertRejected(KOBOLD_WARRIOR.replace("\"small\"", "\"colossal\""), "size");
        assertRejected(KOBOLD_WARRIOR.replace("\"level\":-1", "\"level\":30"), "level");
        assertRejected(KOBOLD_WARRIOR.replace("\"1d6\"", "\"3d7\""), "dice");
        assertRejected(KOBOLD_WARRIOR.replace("\"piercing\"", "\"lava\""), "type");
        assertRejected(KOBOLD_WARRIOR.replace("\"ac\":16,", ""), "ac");
        IllegalArgumentException dup = assertThrows(IllegalArgumentException.class,
                () -> MonsterParser.parse("[" + KOBOLD_WARRIOR + "," + KOBOLD_WARRIOR + "]"));
        assertTrue(dup.getMessage().contains("kobold-warrior"));
    }
}
