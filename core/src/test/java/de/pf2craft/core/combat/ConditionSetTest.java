package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConditionSetTest {
    @Test
    void applyKeepsHigher() {
        ConditionSet s = new ConditionSet();
        s.apply(Condition.FRIGHTENED, 2);
        s.apply(Condition.FRIGHTENED, 1);
        assertEquals(2, s.value(Condition.FRIGHTENED));
    }

    @Test
    void decrementRemovesAtZero() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.FRIGHTENED, 1);
        s.decrement(Condition.FRIGHTENED);
        assertFalse(s.has(Condition.FRIGHTENED));
    }

    @Test
    void unvaluedClampsToOne() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.PRONE, 5);
        assertEquals(1, s.value(Condition.PRONE));
    }

    @Test
    void setZeroRemoves() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.SLOWED, 2);
        s.set(Condition.SLOWED, 0);
        assertFalse(s.has(Condition.SLOWED));
        assertTrue(s.isEmpty());
    }

    @Test
    void allKeepsInsertionOrder() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.PRONE, 1);
        s.set(Condition.FRIGHTENED, 2);
        s.set(Condition.GRABBED, 1);
        assertEquals(List.of(Condition.PRONE, Condition.FRIGHTENED, Condition.GRABBED),
                List.copyOf(s.all().keySet()));
    }

    @Test
    void dyingIsCappedAtFour() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.DYING, 7);
        assertEquals(4, s.value(Condition.DYING));
    }

    @Test
    void byIdAndFlags() {
        assertEquals(Condition.OFF_GUARD, Condition.byId("off_guard").orElseThrow());
        assertTrue(Condition.byId("nope").isEmpty());
        assertTrue(Condition.PRONE.publicToAll());
        assertFalse(Condition.FRIGHTENED.publicToAll());
    }

    @Test
    void copyIsIndependent() {
        ConditionSet s = new ConditionSet();
        s.set(Condition.PRONE, 1);
        ConditionSet c = s.copy();
        c.remove(Condition.PRONE);
        assertTrue(s.has(Condition.PRONE));
    }
}
