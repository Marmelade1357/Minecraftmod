package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.pf2craft.core.check.DegreeOfSuccess;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpellResolverTest {
    private static SpellDefinition spell(int rank, String dice, String heighten, int step) {
        return new SpellDefinition("x", "X", rank, 2, 30, 1, SpellKind.SAVE, "reflex", true, dice, 0, "fire",
                heighten, step, false, 0, "", "", "");
    }

    @Test
    void cantripHeightening() {
        SpellDefinition d = spell(0, "2d4", "1d4", 1);
        assertEquals("2d4", SpellResolver.damageAtRank(d, 1));
        assertEquals("4d4", SpellResolver.damageAtRank(d, 3));
    }

    @Test
    void rankedHeightening() {
        SpellDefinition d = spell(3, "6d6", "2d6", 1);
        assertEquals("6d6", SpellResolver.damageAtRank(d, 3));
        assertEquals("10d6", SpellResolver.damageAtRank(d, 5));
    }

    @Test
    void stepOfTwoAndNoneAndMixedDice() {
        SpellDefinition d = spell(1, "1d8", "1d8", 2);
        assertEquals("1d8", SpellResolver.damageAtRank(d, 2));
        assertEquals("2d8", SpellResolver.damageAtRank(d, 3));
        assertEquals("2d8", SpellResolver.damageAtRank(d, 4));
        assertEquals("1d8", SpellResolver.damageAtRank(spell(1, "1d8", "", 0), 9));
        assertEquals("1d12+2d4", SpellResolver.damageAtRank(spell(1, "1d12", "1d4", 1), 3));
    }

    @Test
    void multipliers() {
        var cs = DegreeOfSuccess.CRITICAL_SUCCESS;
        var s = DegreeOfSuccess.SUCCESS;
        var f = DegreeOfSuccess.FAILURE;
        var cf = DegreeOfSuccess.CRITICAL_FAILURE;
        assertEquals(2, SpellResolver.multiplier(SpellKind.ATTACK, false, cs));
        assertEquals(1, SpellResolver.multiplier(SpellKind.ATTACK, false, s));
        assertEquals(0, SpellResolver.multiplier(SpellKind.ATTACK, false, f));
        assertEquals(0, SpellResolver.multiplier(SpellKind.ATTACK, false, cf));
        assertEquals(0, SpellResolver.multiplier(SpellKind.SAVE, true, cs));
        assertEquals(0.5, SpellResolver.multiplier(SpellKind.SAVE, true, s));
        assertEquals(1, SpellResolver.multiplier(SpellKind.SAVE, true, f));
        assertEquals(2, SpellResolver.multiplier(SpellKind.SAVE, true, cf));
        assertEquals(0, SpellResolver.multiplier(SpellKind.SAVE, false, s));
        assertEquals(1, SpellResolver.multiplier(SpellKind.SAVE, false, f));
        assertEquals(2, SpellResolver.multiplier(SpellKind.SAVE, false, cf));
    }

    @Test
    void autoAlwaysFactorOne() {
        for (DegreeOfSuccess d : DegreeOfSuccess.values()) {
            assertEquals(1, SpellResolver.multiplier(SpellKind.AUTO, false, d));
        }
    }

    @Test
    void damageAtIncludesHeightenBonusAndExtraComponent() {
        SpellDefinition barrage = new SpellDefinition("fb", "FB", 1, 2, 120, 1, SpellKind.AUTO, "", false, "2d4", 2,
                "force", "2d4", 2, false, 2, "", "", "");
        assertEquals(List.of(new SpellResolver.DamageRoll("2d4", 2, "force")), SpellResolver.damageAt(barrage, 1));
        assertEquals(List.of(new SpellResolver.DamageRoll("4d4", 4, "force")), SpellResolver.damageAt(barrage, 3));
        SpellDefinition heal = new SpellDefinition("h", "H", 1, 2, 30, 1, SpellKind.HEAL, "", false, "1d8", 8, "",
                "1d8", 1, false, 8, "", "", "");
        assertEquals(List.of(new SpellResolver.DamageRoll("2d8", 16, "")), SpellResolver.damageAt(heal, 2));
        SpellDefinition ts = new SpellDefinition("t", "T", 1, 2, 120, 1, SpellKind.SAVE, "reflex", true, "1d12", 0,
                "electricity", "1d12", 1, false, 0, "1d4", "sonic", "1d4");
        assertEquals(List.of(new SpellResolver.DamageRoll("2d12", 0, "electricity"),
                new SpellResolver.DamageRoll("2d4", 0, "sonic")), SpellResolver.damageAt(ts, 2));
        assertEquals(List.of(), SpellResolver.damageAt(spell(1, "", "", 0), 3));
    }

    @Test
    void applyFactorRoundsDown() {
        assertEquals(3, SpellResolver.applyFactor(7, 0.5));
        assertEquals(14, SpellResolver.applyFactor(7, 2));
        assertEquals(0, SpellResolver.applyFactor(7, 0));
    }
}
