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
 * Modelling notes: Force Barrage auto-hits (modelled as ATTACK with 2 shards, 1d4+1 each, flat bonus not heightened);
 * Heal and Harm use the 2-action single-target variant (Heal 1d8+8); Thunderstrike keeps only the electricity die
 * (the 1d4 sonic part is dropped); persistent damage and critical-failure riders are left to the GM; Bless, Shield,
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
    void fireballHeightens() throws IOException {
        var f = load().catalog().byName("Fireball").orElseThrow();
        assertEquals("10d6", SpellResolver.damageAtRank(f, 5));
    }
}
