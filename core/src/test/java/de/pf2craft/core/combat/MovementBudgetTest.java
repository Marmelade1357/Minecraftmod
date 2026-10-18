package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MovementBudgetTest {

    @Test
    void blocksPerStrideFollowsSpeed() {
        assertEquals(5, new MovementBudget(25).blocksPerStride());
        assertEquals(1, new MovementBudget(3).blocksPerStride());
        assertEquals(0, new MovementBudget(0).blocksPerStride());
    }

    @Test
    void halfBlockStepsSpendOneActionPerStride() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        for (int i = 0; i < 10; i++) {
            assertTrue(budget.tryMove(0.5, turn));
        }
        assertEquals(2, turn.actionsLeft());
        assertTrue(budget.tryMove(0.5, turn));
        assertEquals(1, turn.actionsLeft());
    }

    @Test
    void exhaustsAfterThreeStrides() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        for (int i = 0; i < 30; i++) {
            assertTrue(budget.tryMove(0.5, turn), "step " + i);
        }
        assertEquals(0, turn.actionsLeft());
        assertFalse(budget.tryMove(0.5, turn));
        assertEquals(0, turn.actionsLeft());
    }

    @Test
    void onlyOneStrideAfterSpendingTwoActions() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(turn.spendActions(2));
        for (int i = 0; i < 10; i++) {
            assertTrue(budget.tryMove(0.5, turn));
        }
        assertFalse(budget.tryMove(0.5, turn));
    }

    @Test
    void speedZeroNeverMoves() {
        TurnState turn = new TurnState();
        assertFalse(new MovementBudget(0).tryMove(0.5, turn));
        assertEquals(3, turn.actionsLeft());
    }

    @Test
    void nonPositiveMoveIsFree() {
        TurnState turn = new TurnState();
        assertTrue(new MovementBudget(25).tryMove(0, turn));
        assertEquals(3, turn.actionsLeft());
    }

    @Test
    void stridesNeeded() {
        MovementBudget budget = new MovementBudget(25);
        assertEquals(1, budget.stridesNeededFor(5));
        assertEquals(2, budget.stridesNeededFor(5.1));
        assertEquals(3, budget.stridesNeededFor(12));
    }

    @Test
    void spendingTooManyActionsChangesNothing() {
        TurnState turn = new TurnState();
        assertFalse(turn.spendActions(4));
        assertEquals(3, turn.actionsLeft());
    }
}
