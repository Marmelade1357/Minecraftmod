package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.combat.DamageResolver;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Modelling notes: Force Barrage auto-hits (kind AUTO) and is modelled as the 2-action use with 2 shards of 1d4+1
 * (the 1- and 3-action variants are simplified away; +2 ranks adds one shard per action = +2d4+2);
 * Heal and Harm use the 2-action single-target variant (Heal 1d8+8, +1d8+8 per rank); Thunderstrike has a second
 * component (1d4 sonic); persistent damage and critical-failure riders are left to the GM; Bless, Shield,
 * Sure Strike are UTILITY.
 */
class StarterCatalogTest {
    private static SpellCatalogParser.ParseResult load() throws IOException {
        try (InputStream in = StarterCatalogTest.class.getResourceAsStream("/pf2craft/spells/spells.json")) {
            return SpellCatalogParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void parsesWithoutErrors() throws IOException {
        var r = load();
        assertEquals(List.of(), r.errors());
        assertTrue(r.catalog().all().size() >= 25);
    }

    @Test
    void containsRequiredSpells() throws IOException {
        var c = load().catalog();
        for (String n : List.of("Electric Arc", "Force Barrage", "Heal", "Harm", "Fireball", "Breathe Fire",
                "Ignition", "Frostbite", "Caustic Blast", "Needle Darts", "Telekinetic Projectile", "Divine Lance",
                "Gale Blast", "Slashing Gust", "Thunderstrike", "Chilling Spray", "Daze", "Fear", "Shield",
                "Sure Strike", "Bless", "Lightning Bolt", "Horizon Thunder Sphere", "Spout", "Puff of Poison")) {
            assertTrue(c.byName(n).isPresent(), n);
        }
    }

    @Test
    void damageSpellsHaveValidTypes() throws IOException {
        for (SpellDefinition d : load().catalog().all()) {
            if (!d.damageDice().isEmpty() && d.kind() != SpellKind.HEAL) {
                assertTrue(DamageResolver.isDamageType(d.damageType()), d.name());
            }
        }
    }

    @Test
    void fixedSpellValues() throws IOException {
        var c = load().catalog();
        var barrage = c.byName("Force Barrage").orElseThrow();
        assertEquals(SpellKind.AUTO, barrage.kind());
        assertEquals("4d4", SpellResolver.damageAt(barrage, 3).get(0).dice());
        assertEquals(4, SpellResolver.damageAt(barrage, 3).get(0).bonus());
        var heal = SpellResolver.damageAt(c.byName("Heal").orElseThrow(), 2).get(0);
        assertEquals("2d8", heal.dice());
        assertEquals(16, heal.bonus());
        var ts = SpellResolver.damageAt(c.byName("Thunderstrike").orElseThrow(), 2);
        assertEquals("2d12", ts.get(0).dice());
        assertEquals("electricity", ts.get(0).type());
        assertEquals("2d4", ts.get(1).dice());
        assertEquals("sonic", ts.get(1).type());
        assertTrue(c.byName("Gale Blast").orElseThrow().basicSave());
        assertTrue(c.byName("Chilling Spray").orElseThrow().basicSave());
    }

    @Test
    void fireballHeightens() throws IOException {
        var f = load().catalog().byName("Fireball").orElseThrow();
        assertEquals("10d6", SpellResolver.damageAtRank(f, 5));
    }
}
