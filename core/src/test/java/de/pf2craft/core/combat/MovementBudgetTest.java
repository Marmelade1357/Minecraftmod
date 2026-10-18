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

    @Test
    void tenthBlockStepsAreNotBrokenByRounding() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        for (int i = 0; i < 50; i++) {
            assertTrue(budget.tryMove(0.1, turn));
        }
        assertEquals(2, turn.actionsLeft());
        assertTrue(budget.tryMove(0.1, turn));
        assertEquals(1, turn.actionsLeft());
    }

    @Test
    void resetMakesNextMoveCostAnAction() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.tryMove(1, turn));
        budget.reset();
        assertTrue(budget.tryMove(1, turn));
        assertEquals(1, turn.actionsLeft());
    }

    @Test
    void stridesNeededAtSpeedZero() {
        assertEquals(Integer.MAX_VALUE, new MovementBudget(0).stridesNeededFor(3));
    }

    @Test
    void payWalkSpendsStridesAndKeepsTheRest() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.payWalk(4, turn));
        assertEquals(2, turn.actionsLeft());
        assertEquals(1.0, budget.remainingInStride(), 1e-9);
        // 1 block is still free, then the next stride is needed
        assertTrue(budget.payWalk(1, turn));
        assertEquals(2, turn.actionsLeft());
        assertEquals(0.0, budget.remainingInStride(), 1e-9);
        assertTrue(budget.payWalk(6, turn));
        assertEquals(0, turn.actionsLeft());
        assertEquals(4.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void payWalkRefusedChangesNothing() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.payWalk(2, turn));
        assertFalse(budget.payWalk(40, turn));
        assertEquals(2, turn.actionsLeft());
        assertEquals(3.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void payWalkZeroAndSpeedZero() {
        TurnState turn = new TurnState();
        assertTrue(new MovementBudget(25).payWalk(0, turn));
        assertEquals(3, turn.actionsLeft());
        assertFalse(new MovementBudget(0).payWalk(2, turn));
        assertEquals(3, turn.actionsLeft());
    }
}
