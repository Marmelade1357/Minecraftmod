package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.check.DegreeOfSuccess;
import org.junit.jupiter.api.Test;

class DyingRulesTest {
    private static ConditionSet dying(int value) {
        ConditionSet s = new ConditionSet();
        s.set(Condition.UNCONSCIOUS, 1);
        s.set(Condition.DYING, value);
        return s;
    }

    @Test
    void knockOutNormalGivesDying1() {
        ConditionSet s = new ConditionSet();
        DyingRules.knockOut(s, false);
        assertEquals(1, s.value(Condition.DYING));
        assertTrue(s.has(Condition.UNCONSCIOUS));
    }

    @Test
    void knockOutCriticalGivesDying2() {
        ConditionSet s = new ConditionSet();
        DyingRules.knockOut(s, true);
        assertEquals(2, s.value(Condition.DYING));
    }

    @Test
    void woundedRaisesDying() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.WOUNDED, 2);
        DyingRules.knockOut(s, false);
        assertEquals(3, s.value(Condition.DYING));
    }

    @Test
    void recoverSuccessLowersByOne() {
        ConditionSet s = dying(3);
        DyingRules.RecoveryResult r = DyingRules.recover(s, DegreeOfSuccess.SUCCESS);
        assertEquals(2, s.value(Condition.DYING));
        assertFalse(r.stabilized());
        assertFalse(r.wouldBeDead());
    }

    @Test
    void recoverCritSuccessLowersByTwo() {
        ConditionSet s = dying(3);
        DyingRules.recover(s, DegreeOfSuccess.CRITICAL_SUCCESS);
        assertEquals(1, s.value(Condition.DYING));
    }

    @Test
    void recoverFailureRaises() {
        ConditionSet s = dying(1);
        DyingRules.recover(s, DegreeOfSuccess.FAILURE);
        assertEquals(2, s.value(Condition.DYING));
    }

    @Test
    void recoverCritFailureRaisesTwo() {
        ConditionSet s = dying(1);
        DyingRules.recover(s, DegreeOfSuccess.CRITICAL_FAILURE);
        assertEquals(3, s.value(Condition.DYING));
    }

    @Test
    void stabilizingKeepsUnconsciousAndRaisesWounded() {
        ConditionSet s = dying(1);
        DyingRules.RecoveryResult r = DyingRules.recover(s, DegreeOfSuccess.SUCCESS);
        assertTrue(r.stabilized());
        assertFalse(s.has(Condition.DYING));
        assertTrue(s.has(Condition.UNCONSCIOUS));
        assertEquals(1, s.value(Condition.WOUNDED));
    }

    @Test
    void dyingFourReportsWouldBeDeadOnceAndKeepsCondition() {
        ConditionSet s = dying(3);
        DyingRules.RecoveryResult r = DyingRules.recover(s, DegreeOfSuccess.FAILURE);
        assertTrue(r.wouldBeDead());
        assertEquals(4, s.value(Condition.DYING));
        assertTrue(s.has(Condition.UNCONSCIOUS));
        // a further failure stays capped at 4
        DyingRules.recover(s, DegreeOfSuccess.CRITICAL_FAILURE);
        assertEquals(4, s.value(Condition.DYING));
    }

    @Test
    void healRemovesDyingAndUnconsciousAndRaisesWounded() {
        ConditionSet s = dying(2);
        DyingRules.heal(s);
        assertFalse(s.has(Condition.DYING));
        assertFalse(s.has(Condition.UNCONSCIOUS));
        assertEquals(1, s.value(Condition.WOUNDED));
    }

    @Test
    void healWithoutDyingKeepsWounded() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.UNCONSCIOUS, 1);
        DyingRules.heal(s);
        assertFalse(s.has(Condition.UNCONSCIOUS));
        assertFalse(s.has(Condition.WOUNDED));
    }

    @Test
    void recoveryDcIs10PlusDying() {
        assertEquals(13, DyingRules.recoveryDc(dying(3)));
    }
}
