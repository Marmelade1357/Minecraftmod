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

    @Test
    void interruptLosesTheRestOfTheStride() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.tryMove(2, turn));
        assertEquals(3.0, budget.remainingInStride(), 1e-9);
        budget.interrupt();
        assertEquals(0.0, budget.remainingInStride(), 1e-9);
        assertEquals(2, turn.actionsLeft());
        assertTrue(budget.tryMove(1, turn)); // a new Stride: one more action
        assertEquals(1, turn.actionsLeft());
        assertEquals(4.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void interruptWithoutActionsBlocksMovement() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.tryMove(1, turn));
        turn.spendActions(2);
        budget.interrupt();
        assertFalse(budget.tryMove(1, turn));
    }

    @Test
    void stepGivesExactlyOneBlock() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        budget.grantStep();
        assertTrue(budget.tryMove(0.5, turn));
        assertTrue(budget.tryMove(0.5, turn));
        assertEquals(3, turn.actionsLeft());
        assertTrue(budget.tryMove(0.5, turn)); // a normal Stride starts and costs an action
        assertEquals(2, turn.actionsLeft());
        assertEquals(4.5, budget.remainingInStride(), 1e-9);
    }

    @Test
    void stepDoesNotBreakRunningStride() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.tryMove(2, turn));
        assertEquals(3.0, budget.remainingInStride(), 1e-9);
        budget.grantStep();
        assertEquals(3.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void stepAfterInterruptGivesOneBlock() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.tryMove(2, turn));
        budget.interrupt();
        budget.grantStep();
        assertEquals(1.0, budget.remainingInStride(), 1e-9);
        assertTrue(budget.tryMove(1, turn));
        assertEquals(2, turn.actionsLeft());
    }

    @Test
    void startStrideSpendsOneActionAndFillsBudget() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.startStride(turn));
        assertEquals(2, turn.actionsLeft());
        assertEquals(5.0, budget.remainingInStride(), 1e-9);
        assertTrue(budget.strideActive());
    }

    @Test
    void startStrideRefusedWithoutActions() {
        TurnState turn = new TurnState();
        turn.spendActions(3);
        MovementBudget budget = new MovementBudget(25);
        assertFalse(budget.startStride(turn));
        assertFalse(budget.strideActive());
        assertFalse(new MovementBudget(0).startStride(new TurnState()));
    }

    @Test
    void startStrideRefusedWhileActive() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        assertTrue(budget.startStride(turn));
        assertFalse(budget.startStride(turn));
        assertEquals(2, turn.actionsLeft());
        assertEquals(5.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void moveWithinStrideWithoutStrideIsRefused() {
        MovementBudget budget = new MovementBudget(25);
        assertFalse(budget.moveWithinStride(1));
        assertEquals(0.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void moveWithinStrideConsumesBlocks() {
        MovementBudget budget = new MovementBudget(25);
        budget.startStride(new TurnState());
        assertTrue(budget.moveWithinStride(2));
        assertEquals(3.0, budget.remainingInStride(), 1e-9);
        assertFalse(budget.moveWithinStride(10));
        assertEquals(3.0, budget.remainingInStride(), 1e-9);
    }

    @Test
    void moveWithinStrideAllowsOneStepOvershootThenEnds() {
        MovementBudget budget = new MovementBudget(25);
        budget.startStride(new TurnState());
        assertTrue(budget.moveWithinStride(4.6));
        assertEquals(0.4, budget.remainingInStride(), 1e-9);
        assertTrue(budget.moveWithinStride(1));
        assertEquals(0.0, budget.remainingInStride(), 1e-9);
        assertFalse(budget.strideActive());
        assertFalse(budget.moveWithinStride(0.5));
    }

    @Test
    void grantedStepCanBeUsedWithoutStartingStride() {
        MovementBudget budget = new MovementBudget(25);
        budget.grantStep();
        assertTrue(budget.strideActive());
        assertTrue(budget.moveWithinStride(1));
        assertFalse(budget.strideActive());
    }

    @Test
    void interruptEndsStride() {
        MovementBudget budget = new MovementBudget(25);
        budget.startStride(new TurnState());
        budget.interrupt();
        assertFalse(budget.strideActive());
        assertFalse(budget.moveWithinStride(1));
    }

    @Test
    void interruptClearsTheStepMarker() {
        TurnState turn = new TurnState();
        MovementBudget budget = new MovementBudget(25);
        budget.grantStep();
        budget.interrupt();
        assertTrue(budget.tryMove(1, turn));
        assertEquals(2, turn.actionsLeft());
    }
}
