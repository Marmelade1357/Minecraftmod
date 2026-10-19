package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConditionEffectsTest {
    @Test
    void frightenedPenalizesEverything() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.FRIGHTENED, 2);
        assertEquals(-2, ConditionEffects.checkPenalty(s));
        assertEquals(-2, ConditionEffects.acPenalty(s));
        assertEquals(-2, ConditionEffects.attackPenalty(s));
    }

    @Test
    void proneHurtsAcAndAttacks() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.PRONE, 1);
        assertEquals(-2, ConditionEffects.acPenalty(s));
        assertEquals(-2, ConditionEffects.attackPenalty(s));
        assertEquals(0, ConditionEffects.checkPenalty(s));
    }

    @Test
    void circumstanceAcPenaltyAppliesOnce() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.PRONE, 1);
        s.set(Condition.OFF_GUARD, 1);
        s.set(Condition.GRABBED, 1);
        assertEquals(-2, ConditionEffects.acPenalty(s));
    }

    @Test
    void actionsLostIsCappedAtThree() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.SLOWED, 1);
        s.set(Condition.STUNNED, 3);
        assertEquals(3, ConditionEffects.actionsLost(s));
    }

    @Test
    void dyingCannotAct() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.DYING, 1);
        assertFalse(ConditionEffects.canAct(s));
    }

    @Test
    void emptySetHasNoEffects() {
        ConditionSet s = new ConditionSet();
        assertEquals(0, ConditionEffects.checkPenalty(s));
        assertEquals(0, ConditionEffects.acPenalty(s));
        assertEquals(0, ConditionEffects.attackPenalty(s));
        assertEquals(0, ConditionEffects.actionsLost(s));
        assertTrue(ConditionEffects.canAct(s));
    }
}
