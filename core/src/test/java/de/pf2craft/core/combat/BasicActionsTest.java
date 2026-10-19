package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BasicActionsTest {
    @Test
    void allHasTheSevenActions() {
        assertEquals(List.of("step", "raise_shield", "take_cover", "seek", "demoralize", "trip", "grapple"),
                BasicActions.ALL.stream().map(BasicAction::id).toList());
    }

    @Test
    void tripIsAMeleeAttackAgainstReflex() {
        BasicAction trip = BasicActions.byId("trip").orElseThrow();
        assertEquals(1, trip.cost());
        assertEquals(ActionKind.TARGET_MELEE, trip.kind());
        assertTrue(trip.countsAsAttack());
        assertEquals("reflex", trip.defense());
        assertEquals("athletics", trip.skill());
    }

    @Test
    void demoralizeIsRangedSixBlocks() {
        BasicAction d = BasicActions.byId("demoralize").orElseThrow();
        assertEquals(ActionKind.TARGET_RANGE, d.kind());
        assertEquals(6, d.rangeBlocks());
        assertEquals("will", d.defense());
        assertFalse(d.countsAsAttack());
    }

    @Test
    void raiseShieldNeedsShield() {
        assertTrue(BasicActions.byId("raise_shield").orElseThrow().needsShield());
        assertFalse(BasicActions.byId("step").orElseThrow().needsShield());
    }

    @Test
    void unknownIdIsEmpty() {
        assertTrue(BasicActions.byId("nope").isEmpty());
    }
}
