package de.pf2craft.core.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Sanity checks for the bundled Monster Core data file. */
class MonsterDataConsistencyTest {
    // Reason per creature without a Strike (checked against Foundry actors and AoN): swarms attack only
    // through area abilities (Swarming Bites etc.), they have no Melee/Ranged line.
    private static final Map<String, String> NO_STRIKES = Map.of(
            "army-ant-swarm", "swarm: area ability only",
            "centipede-swarm", "swarm: area ability only",
            "rat-swarm", "swarm: area ability only",
            "scorpion-swarm", "swarm: area ability only",
            "spider-swarm", "swarm: area ability only",
            "tooth-fairy-swarm", "swarm: area ability only",
            "vampire-bat-swarm", "swarm: area ability only",
            "vescavor-swarm", "swarm: area ability only",
            "wasp-swarm", "swarm: area ability only");
    // Monsters allowed to have speed 0 (immobile); none in Monster Core, fly/swim-only ones use their best speed.
    private static final Map<String, String> ZERO_SPEED = Map.of();

    private static List<MonsterData> all;
    private static Map<String, MonsterData> byId;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = MonsterDataConsistencyTest.class
                .getResourceAsStream("/pf2craft/monsters/monster-core.json")) {
            assertNotNull(in, "monster-core.json missing");
            all = MonsterParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        byId = all.stream().collect(Collectors.toMap(MonsterData::id, Function.identity()));
    }

    @Test
    void hasEnoughMonsters() {
        assertTrue(all.size() >= 350, "size " + all.size());
    }

    // Spot values from Archives of Nethys (Remaster entries):
    // https://2e.aonprd.com/NPCs.aspx?ID=3072 (Kobold Warrior: AC 16, HP 7, Fort +3, spear +3/+5/+5)
    // https://2e.aonprd.com/Monsters.aspx?ID=3241 (Wolf)
    // https://2e.aonprd.com/Monsters.aspx?ID=3119 (Ogre Glutton)
    // https://2e.aonprd.com/Monsters.aspx?ID=2923 (Tyrannosaurus)
    // https://2e.aonprd.com/Monsters.aspx?ID=2940 (Ancient Diabolic Dragon; its Jaws +38 is listed
    // among the attack bonuses 36/38/38)
    @Test
    void spotValues() {
        spot("kobold-warrior", -1, 16, 7, 3, 3, "Spear");
        spot("wolf", 1, 15, 24, 6, 9, "Jaws");
        spot("ogre-glutton", 4, 18, 70, 14, 14, "Greataxe");
        spot("tyrannosaurus", 10, 29, 180, 21, 22, "Jaws");
        spot("diabolic-dragon-ancient", 20, 44, 390, 36, 38, "Jaws");
        assertEquals(33, byId.get("diabolic-dragon-ancient").perception());
        assertEquals(19, byId.get("tyrannosaurus").perception());
        assertEquals(7, byId.get("wolf").perception());
        assertEquals(6, byId.get("ogre-glutton").perception());
        assertEquals(3, byId.get("kobold-warrior").perception());
    }

    private static void spot(String id, int level, int ac, int hp, int fort, int attack, String strike) {
        MonsterData m = byId.get(id);
        assertNotNull(m, id);
        assertEquals(level, m.level(), id + " level");
        assertEquals(ac, m.ac(), id + " ac");
        assertEquals(hp, m.hp(), id + " hp");
        assertEquals(fort, m.fortitude(), id + " fortitude");
        assertEquals(strike, m.strikes().get(0).name(), id + " strike name");
        assertEquals(attack, m.strikes().get(0).attackBonus(), id + " attack");
    }

    @Test
    void strikesAndLimits() {
        for (MonsterData m : all) {
            if (m.strikes().isEmpty()) {
                assertTrue(NO_STRIKES.containsKey(m.id()), "unexpected monster without strikes: " + m.id());
            }
            for (String a : m.abilities()) {
                assertTrue(a.length() <= 64, m.id() + " ability too long: " + a);
            }
        }
        assertTrue(NO_STRIKES.size() * 20 <= all.size(), "NO_STRIKES above 5 %");
    }

    @Test
    void reloadAndVolleyStrikesAreRanged() {
        for (MonsterData m : all) {
            for (MonsterStrike s : m.strikes()) {
                boolean rangedTrait = s.traits().stream().anyMatch(t -> t.startsWith("reload-")
                        || t.startsWith("volley-") || t.equals("propulsive"));
                assertTrue(!rangedTrait || s.ranged(), m.id() + " " + s.name() + " should be ranged");
            }
        }
        assertTrue(byId.get("kobold-warrior").strikes().stream().anyMatch(s -> s.name().equals("Sling")
                && s.ranged()));
        assertEquals(2, byId.get("weretiger").strikes().size());
    }

    @Test
    void speedIsNeverZeroUnlessListed() {
        for (MonsterData m : all) {
            assertTrue(m.speed() > 0 || ZERO_SPEED.containsKey(m.id()), m.id() + " has speed 0");
        }
        assertEquals(60, byId.get("banshee").speed());
        assertEquals(80, byId.get("megalodon").speed());
    }

    @Test
    void noDuplicateListEntries() {
        for (MonsterData m : all) {
            assertEquals(m.immunities().size(), Set.copyOf(m.immunities()).size(), m.id() + " immunities");
            assertEquals(m.traits().size(), Set.copyOf(m.traits()).size(), m.id() + " traits");
        }
    }

    @Test
    void noLeakedDescriptionMarkup() throws IOException {
        try (InputStream in = MonsterDataConsistencyTest.class
                .getResourceAsStream("/pf2craft/monsters/monster-core.json")) {
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(!raw.contains("@UUID") && !raw.contains("<p>"));
        }
    }
}
