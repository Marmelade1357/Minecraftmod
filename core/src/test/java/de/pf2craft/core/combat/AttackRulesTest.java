package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.check.DegreeOfSuccess;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class AttackRulesTest {
    @Test
    void mapPenalties() {
        assertEquals(0, AttackMath.mapPenalty(1, false));
        assertEquals(-5, AttackMath.mapPenalty(2, false));
        assertEquals(-10, AttackMath.mapPenalty(3, false));
        assertEquals(-4, AttackMath.mapPenalty(2, true));
        assertEquals(-8, AttackMath.mapPenalty(3, true));
        assertEquals(-10, AttackMath.mapPenalty(4, false));
        assertEquals(0, AttackMath.mapPenalty(1, true));
    }

    @Test
    void resolveAppliesPenaltiesAndDegrees() {
        AttackOutcome hit = AttackMath.resolve(15, 7, -5, 0, 17);
        assertEquals(17, hit.total());
        assertEquals(15, hit.natural());
        assertEquals(DegreeOfSuccess.SUCCESS, hit.degree());

        AttackOutcome nat20 = AttackMath.resolve(20, 0, 0, 0, 25);
        assertEquals(20, nat20.total());
        assertEquals(DegreeOfSuccess.SUCCESS, nat20.degree());

        // total 21 beats AC 10 by 11 (critical success); the natural 1 lowers it one step
        AttackOutcome nat1 = AttackMath.resolve(1, 20, 0, 0, 10);
        assertEquals(21, nat1.total());
        assertEquals(DegreeOfSuccess.SUCCESS, nat1.degree());

        AttackOutcome miss = AttackMath.resolve(10, 5, -10, -2, 15);
        assertEquals(3, miss.total());
        assertEquals(DegreeOfSuccess.CRITICAL_FAILURE, miss.degree());
    }

    @Test
    void agileTrait() {
        assertTrue(AttackMath.isAgile(List.of("finesse", "agile")));
        assertFalse(AttackMath.isAgile(List.of("reach")));
    }

    @Test
    void reach() {
        assertEquals(1.0, RangeRules.meleeReachBlocks(List.of("agile")));
        assertEquals(2.0, RangeRules.meleeReachBlocks(List.of("reach")));
        assertEquals(2.0, RangeRules.meleeReachBlocks(List.of("reach-10")));
        assertEquals(3.0, RangeRules.meleeReachBlocks(List.of("reach-15")));
        assertEquals(4.0, RangeRules.meleeReachBlocks(List.of("reach-20")));
    }

    @Test
    void rangePenalties() {
        assertEquals(OptionalInt.of(0), RangeRules.rangePenalty(10, 60));
        assertEquals(OptionalInt.of(-2), RangeRules.rangePenalty(13, 60));
        assertEquals(OptionalInt.of(-10), RangeRules.rangePenalty(72, 60));
        assertEquals(OptionalInt.empty(), RangeRules.rangePenalty(73, 60));
        assertEquals(OptionalInt.of(0), RangeRules.rangePenalty(0, 60));
    }

    @Test
    void healthLabels() {
        assertEquals(HealthLabel.UNHURT, HealthLabel.of(10, 10));
        assertEquals(HealthLabel.HURT, HealthLabel.of(9, 10));
        assertEquals(HealthLabel.HURT, HealthLabel.of(5, 10));
        assertEquals(HealthLabel.BADLY_HURT, HealthLabel.of(4, 10));
        assertEquals(HealthLabel.DEFEATED, HealthLabel.of(0, 10));
        assertEquals(HealthLabel.DEFEATED, HealthLabel.of(-3, 10));
    }
}
