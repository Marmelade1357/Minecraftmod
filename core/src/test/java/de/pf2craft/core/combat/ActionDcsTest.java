package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderImporter;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.MonsterParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionDcsTest {
    @Test
    void fighterBonusesAndDcs() throws Exception {
        CharacterSheet s = PathbuilderImporter.parse(read("/pathbuilder/422753-fighter5.json"));
        assertEquals(13, ActionDcs.skillBonus(s, "athletics"));
        assertEquals(0, ActionDcs.skillBonus(s, "intimidation"));
        assertEquals(12, ActionDcs.skillBonus(s, "perception"));
        assertEquals(0, ActionDcs.skillBonus(s, "bogus"));
        assertEquals(19, ActionDcs.defenseDc(s, "reflex").getAsInt());
        assertEquals(22, ActionDcs.defenseDc(s, "will").getAsInt());
        assertEquals(22, ActionDcs.defenseDc(s, "perception_dc").getAsInt());
        assertTrue(ActionDcs.defenseDc(s, "bogus").isEmpty());
    }

    @Test
    void koboldRules() throws Exception {
        MonsterData m = kobold();
        assertEquals(10 + m.reflex(), ActionDcs.defenseDc(m, "reflex").getAsInt());
        assertEquals(10 + m.fortitude(), ActionDcs.defenseDc(m, "fortitude").getAsInt());
        assertEquals(10 + m.will(), ActionDcs.defenseDc(m, "will").getAsInt());
        assertEquals(10 + m.perception(), ActionDcs.defenseDc(m, "perception_dc").getAsInt());
        assertTrue(ActionDcs.defenseDc(m, "bogus").isEmpty());
        int expected = m.skills().containsKey("athletics") ? m.skills().get("athletics")
                : m.strikes().stream().mapToInt(x -> x.attackBonus()).max().getAsInt() - 2;
        assertEquals(expected, ActionDcs.skillBonus(m, "athletics"));
        if (!m.skills().containsKey("intimidation")) {
            assertEquals(m.perception(), ActionDcs.skillBonus(m, "intimidation"));
        }
        assertEquals(m.perception(), ActionDcs.skillBonus(m, "perception"));
    }

    @Test
    void fallbacks() throws Exception {
        MonsterData k = kobold();
        MonsterData bare = new MonsterData("x", "X", 3, "medium", List.of(), 5, 10, 5, 5, 5, 10, 25,
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        assertEquals(7, ActionDcs.skillBonus(bare, "athletics"));
        assertEquals(7, ActionDcs.skillBonus(bare, "stealth"));
        assertEquals(5, ActionDcs.skillBonus(bare, "intimidation"));
        MonsterData withSkill = new MonsterData("x", "X", 3, "medium", List.of(), 5, 10, 5, 5, 5, 10, 25,
                k.strikes(), List.of(), List.of(), List.of(), Map.of("athletics", 11), List.of());
        assertEquals(11, ActionDcs.skillBonus(withSkill, "athletics"));
    }

    @Test
    void sizeRanks() {
        assertEquals(0, ActionDcs.sizeRank("tiny"));
        assertEquals(2, ActionDcs.sizeRank("medium"));
        assertEquals(5, ActionDcs.sizeRank("gargantuan"));
    }

    private static MonsterData kobold() throws Exception {
        return MonsterParser.parse(read("/pf2craft/monsters/monster-core.json")).stream()
                .filter(m -> m.id().equals("kobold-warrior")).findFirst().orElseThrow();
    }

    private static String read(String path) throws Exception {
        try (InputStream in = ActionDcsTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
